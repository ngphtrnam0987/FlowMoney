package com.flowmoney.app.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowmoney.app.data.local.entity.AccountEntity
import com.flowmoney.app.data.local.entity.CategoryEntity
import com.flowmoney.app.data.local.entity.ReceiptEntity
import com.flowmoney.app.data.local.entity.ReceiptItemEntity
import com.flowmoney.app.data.local.entity.TransactionEntity
import com.flowmoney.app.data.model.AccountType
import com.flowmoney.app.data.model.ClassificationStatus
import com.flowmoney.app.data.model.TransactionDirection
import com.flowmoney.app.data.model.TransactionSource
import com.flowmoney.app.data.repository.AccountRepository
import com.flowmoney.app.data.repository.CategoryRepository
import com.flowmoney.app.data.repository.ReceiptRepository
import com.flowmoney.app.data.repository.TransactionRepository
import com.flowmoney.app.domain.engine.MatchCandidate
import com.flowmoney.app.domain.engine.ParsedReceiptData
import com.flowmoney.app.domain.engine.ReceiptMatcher
import com.flowmoney.app.domain.engine.ReceiptOcrParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val rawOcrText: String? = null,
    val merchantName: String = "",
    val totalAmount: Long = 0L,
    val receiptDate: Long = System.currentTimeMillis(),
    val lineItems: List<ReceiptItemEntity> = emptyList(),
    val matchCandidates: List<MatchCandidate> = emptyList(),
    val selectedMatchTransactionId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val ocrParser: ReceiptOcrParser,
    private val receiptRepository: ReceiptRepository,
    private val transactionRepository: TransactionRepository,
    private val receiptMatcher: ReceiptMatcher,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    init {
        viewModelScope.launch {
            val cats = categoryRepository.getAllCategories()
            val accs = accountRepository.getAllAccounts()
            _uiState.value = _uiState.value.copy(categories = cats, accounts = accs)
        }
    }

    fun processImageUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                val inputImage = withContext(Dispatchers.IO) {
                    val stream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    InputImage.fromBitmap(bitmap, 0)
                }

                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsed = ocrParser.parseOcrText(visionText.text)
                        applyParsedReceipt(parsed)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            errorMessage = "Không thể nhận diện văn bản: ${e.localizedMessage}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Lỗi đọc ảnh: ${e.localizedMessage}"
                )
            }
        }
    }

    fun processBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val parsed = ocrParser.parseOcrText(visionText.text)
                        applyParsedReceipt(parsed)
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isScanning = false,
                            errorMessage = "Không thể nhận diện văn bản: ${e.localizedMessage}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = "Lỗi xử lý ảnh: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun applyParsedReceipt(parsed: ParsedReceiptData) {
        viewModelScope.launch {
            // Find potential match candidates from bank transactions
            val mockReceipt = ReceiptEntity(
                merchantName = parsed.merchantName,
                totalMinor = parsed.totalMinor,
                receiptDate = parsed.date
            )
            val candidates = receiptMatcher.findMatchingCandidates(mockReceipt)

            _uiState.value = _uiState.value.copy(
                isScanning = false,
                rawOcrText = parsed.rawText,
                merchantName = parsed.merchantName ?: "",
                totalAmount = parsed.totalMinor,
                receiptDate = parsed.date ?: System.currentTimeMillis(),
                lineItems = parsed.lineItems,
                matchCandidates = candidates,
                selectedMatchTransactionId = candidates.firstOrNull()?.transaction?.id
            )
        }
    }

    fun updateMerchantName(name: String) {
        _uiState.value = _uiState.value.copy(merchantName = name)
    }

    fun updateTotalAmount(amount: Long) {
        _uiState.value = _uiState.value.copy(totalAmount = amount)
    }

    fun addLineItem(name: String, price: Long, quantity: Double = 1.0) {
        val current = _uiState.value.lineItems.toMutableList()
        current.add(
            ReceiptItemEntity(
                receiptId = 0L,
                name = name,
                quantity = quantity,
                totalPriceMinor = price,
                unitPriceMinor = price
            )
        )
        val newTotal = current.sumOf { it.totalPriceMinor }
        _uiState.value = _uiState.value.copy(lineItems = current, totalAmount = newTotal)
    }

    fun removeLineItem(index: Int) {
        val current = _uiState.value.lineItems.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val newTotal = current.sumOf { it.totalPriceMinor }
            _uiState.value = _uiState.value.copy(lineItems = current, totalAmount = newTotal)
        }
    }

    fun selectMatchTransaction(transactionId: Long?) {
        _uiState.value = _uiState.value.copy(selectedMatchTransactionId = transactionId)
    }

    /**
     * Confirms receipt:
     * - Saves receipt and line items to database
     * - If matched to a bank transaction: links receipt without double counting
     * - If not matched: creates a new manual expense transaction for this receipt
     */
    fun saveReceipt(createTransactionIfNotMatched: Boolean = true) {
        val currentState = _uiState.value
        if (currentState.totalAmount <= 0L) {
            _uiState.value = currentState.copy(errorMessage = "Số tiền hóa đơn phải lớn hơn 0")
            return
        }

        viewModelScope.launch {
            val receipt = ReceiptEntity(
                merchantName = currentState.merchantName.ifBlank { "Hóa đơn" },
                totalMinor = currentState.totalAmount,
                receiptDate = currentState.receiptDate,
                rawOcrText = currentState.rawOcrText
            )
            val receiptId = receiptRepository.insertReceiptWithItems(receipt, currentState.lineItems)

            val matchedTxId = currentState.selectedMatchTransactionId
            if (matchedTxId != null) {
                // Link to existing transaction, prevent double counting
                receiptMatcher.confirmMatch(matchedTxId, receiptId)
            } else if (createTransactionIfNotMatched) {
                // Create a transaction for this receipt
                val defaultAccount = accountRepository.getDefaultAccount()
                    ?: accountRepository.getAllAccounts().firstOrNull()

                val accountId = defaultAccount?.id ?: accountRepository.createAccount(
                    name = "Tiền mặt",
                    type = AccountType.CASH,
                    openingBalanceMinor = 0L,
                    isDefault = true
                )

                val defaultCat = categoryRepository.findByName("Ăn uống")
                    ?: categoryRepository.getAllCategories().firstOrNull()

                val transaction = TransactionEntity(
                    accountId = accountId,
                    source = TransactionSource.RECEIPT_SCAN,
                    amountMinor = currentState.totalAmount,
                    direction = TransactionDirection.EXPENSE,
                    transactionTime = currentState.receiptDate,
                    merchantName = currentState.merchantName.ifBlank { "Hóa đơn" },
                    description = "Quét hóa đơn: ${currentState.merchantName}",
                    categoryId = defaultCat?.id,
                    classificationStatus = ClassificationStatus.CONFIRMED,
                    linkedReceiptId = receiptId
                )
                transactionRepository.insertTransaction(transaction)
            }

            _uiState.value = _uiState.value.copy(isSuccess = true)
        }
    }

    fun resetState() {
        _uiState.value = ScanUiState(
            categories = _uiState.value.categories,
            accounts = _uiState.value.accounts
        )
    }
}
