package com.example.service;

import com.example.config.ConfigProperties;
import com.example.dto.BlockCypherAddressResponse;
import com.example.dto.BlockCypherGenerateAddressResponse;
import com.example.dto.BlockCypherOutput;
import com.example.dto.BlockCypherTransaction;
import com.example.dto.BlockstreamOutput;
import com.example.dto.BlockstreamStatus;
import com.example.dto.BlockstreamTransaction;
import com.example.dto.CryptoConfig;
import com.example.dto.PaymentStatusCheckResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitcoinClient {

    private final ObjectMapper objectMapper;
    private final ConfigProperties config;
    private final RestClient.Builder restClientBuilder;

    /**
     * Parsira crypto config iz JSON stringa
     */
    public CryptoConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, CryptoConfig.class);
        } catch (Exception e) {
            log.error("Error parsing crypto config", e);
            throw new RuntimeException("Invalid crypto configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Konvertuje fiat valutu (EUR, USD, itd.) u Bitcoin (BTC)
     * Koristi CoinGecko API za dobijanje trenutne cene Bitcoin-a
     * 
     * @param fiatAmount Iznos u fiat valuti (npr. 100.00)
     * @param fiatCurrency Fiat valuta (npr. "EUR", "USD")
     * @return Iznos u Bitcoin-u (BTC)
     */
    public BigDecimal convertToBitcoin(BigDecimal fiatAmount, String fiatCurrency) {
        log.info("Converting {} {} to BTC", fiatAmount, fiatCurrency);
        
        try {
            // Pozovi CoinGecko API da dobiješ trenutnu cenu Bitcoin-a
            // GET https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=eur
            RestClient restClient = restClientBuilder
                    .baseUrl("https://api.coingecko.com/api/v3")
                    .build();
            
            String currencyLower = fiatCurrency.toLowerCase();
            Map<String, Object> response = restClient.get()
                    .uri("/simple/price?ids=bitcoin&vs_currencies={currency}", currencyLower)
                    .retrieve()
                    .body(Map.class);
            
            if (response == null || !response.containsKey("bitcoin")) {
                log.error("CoinGecko API did not return bitcoin price. Response: {}", response);
                throw new RuntimeException("Failed to get Bitcoin price from CoinGecko API");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> bitcoinData = (Map<String, Object>) response.get("bitcoin");
            
            if (!bitcoinData.containsKey(currencyLower)) {
                log.error("CoinGecko API did not return price for currency {}. Response: {}", currencyLower, bitcoinData);
                throw new RuntimeException("Currency " + fiatCurrency + " not supported by CoinGecko API");
            }
            
            // Dobij cenu 1 BTC u fiat valuti
            Object priceObj = bitcoinData.get(currencyLower);
            BigDecimal bitcoinPrice;
            
            if (priceObj instanceof Number) {
                bitcoinPrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                bitcoinPrice = new BigDecimal((String) priceObj);
            } else {
                log.error("Invalid price format from CoinGecko API: {}", priceObj);
                throw new RuntimeException("Invalid price format from CoinGecko API");
            }
            
            log.info("Bitcoin price: 1 BTC = {} {}", bitcoinPrice, fiatCurrency);
            
            // Izračunaj: fiatAmount / bitcoinPrice = BTC iznos
            BigDecimal bitcoinAmount = fiatAmount.divide(bitcoinPrice, 8, RoundingMode.HALF_UP);
            
            log.info("Converted {} {} = {} BTC", fiatAmount, fiatCurrency, bitcoinAmount);
            
            return bitcoinAmount;
            
        } catch (Exception e) {
            log.error("Error converting {} {} to BTC", fiatAmount, fiatCurrency, e);
            throw new RuntimeException("Failed to convert " + fiatAmount + " " + fiatCurrency + " to BTC: " + e.getMessage(), e);
        }
    }

    /**
     * Generiše Bitcoin adresu za plaćanje
     * 
     * Strategija:
     * 1. Ako postoji blockcypherApiToken (ili je prazan za testnet), koristi BlockCypher API da generiše novu adresu
     * 2. Ako ne postoji blockcypherApiToken, koristi masterWalletAddress ili walletAddress (fallback)
     * 
     * @param cryptoConfig Merchant-ova crypto konfiguracija
     * @return Bitcoin adresa za plaćanje
     */
    public String generateBitcoinAddress(CryptoConfig cryptoConfig) {
        log.info("Generating Bitcoin address for merchant");
        
        if (cryptoConfig == null) {
            throw new IllegalArgumentException("CryptoConfig cannot be null");
        }
        
        // Strategija 1: Koristi BlockCypher API ako je konfigurisan
        // blockcypherApiToken može biti prazan string "" za testnet (radi bez tokena)
        if (cryptoConfig.getBlockcypherApiToken() != null) {
            try {
                log.info("Using BlockCypher API to generate new Bitcoin address");
                return generateAddressViaBlockCypher(cryptoConfig);
            } catch (Exception e) {
                log.error("Failed to generate address via BlockCypher API: {}. Error type: {}", 
                        e.getMessage(), e.getClass().getSimpleName());
                
                // Fallback: ako BlockCypher API ne radi (nema interneta, DNS greška, itd.), 
                // probaj walletAddress (ako postoji)
                String walletAddress = cryptoConfig.getWalletAddress();
                if (walletAddress != null && !walletAddress.trim().isEmpty()) {
                    log.warn("BlockCypher API failed (likely no internet connection or DNS issue). " +
                            "Using fallback walletAddress: {}", walletAddress);
                    return walletAddress;
                }
                
                // Ako ni walletAddress ne postoji, baci grešku
                throw new RuntimeException(
                        "BlockCypher API failed (likely no internet connection: " + e.getMessage() + ") " +
                        "and no walletAddress fallback provided. " +
                        "Please provide 'walletAddress' in configJson to use static address when offline.", e);
            }
        }
        
        // Strategija 2: Ako BlockCypher API nije konfigurisan, koristi walletAddress (backward compatibility)
        String walletAddress = cryptoConfig.getWalletAddress();
        
        if (walletAddress == null || walletAddress.trim().isEmpty()) {
            log.error("No wallet address configured and BlockCypher API not configured");
            throw new IllegalArgumentException(
                    "Either provide 'blockcypherApiToken' (can be empty string '' for testnet) to generate new addresses via BlockCypher API, " +
                    "or provide 'walletAddress' for static address (backward compatibility).");
        }
        
        log.info("Using static merchant wallet address (BlockCypher API not configured): {}", walletAddress);
        return walletAddress;
    }
    
    /**
     * Generiše novu Bitcoin adresu koristeći BlockCypher API
     * 
     * @param cryptoConfig Merchant-ova crypto konfiguracija
     * @return Nova generisana Bitcoin adresa
     */
    private String generateAddressViaBlockCypher(CryptoConfig cryptoConfig) {
        log.info("Calling BlockCypher API to generate new Bitcoin address");
        
        try {
            // Određujemo network endpoint na osnovu cryptoConfig.network
            String baseUrl = determineNetworkEndpoint(cryptoConfig.getNetwork());
            
            RestClient restClient = restClientBuilder
                    .baseUrl(baseUrl)
                    .build();
            
            // Kreiraj URI sa tokenom ako postoji
            String uri = "/addrs";
            if (cryptoConfig.getBlockcypherApiToken() != null && !cryptoConfig.getBlockcypherApiToken().trim().isEmpty()) {
                uri += "?token=" + cryptoConfig.getBlockcypherApiToken();
            }
            
            log.info("POST {}{}", baseUrl, uri);
            
            // Pozovi BlockCypher API da generiše novu adresu
            // POST /v1/btc/test3/addrs (ili /v1/btc/main/addrs za mainnet)
            BlockCypherGenerateAddressResponse response = restClient.post()
                    .uri(uri)
                    .retrieve()
                    .body(BlockCypherGenerateAddressResponse.class);
            
            if (response == null || response.getAddress() == null || response.getAddress().trim().isEmpty()) {
                throw new RuntimeException("BlockCypher API returned invalid response: address is null or empty");
            }
            
            String newAddress = response.getAddress();
            
            // Sanitize and validate generated address
            if (newAddress != null) {
                newAddress = newAddress.trim();
            }
            
            // Validate address format
            if (newAddress == null || newAddress.isEmpty() || newAddress.length() < 26 || newAddress.length() > 35) {
                log.error("BlockCypher API returned invalid address format: {}", newAddress);
                throw new RuntimeException("BlockCypher API returned invalid address format: " + newAddress);
            }
            
            log.info("Successfully generated new Bitcoin address via BlockCypher API: {} (length: {})", 
                    newAddress, newAddress.length());
            
            return newAddress;
            
        } catch (Exception e) {
            log.error("Error generating Bitcoin address via BlockCypher API", e);
            throw new RuntimeException("Failed to generate Bitcoin address via BlockCypher API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Određuje network endpoint na osnovu network stringa
     * 
     * @param network "testnet" ili "mainnet"
     * @return Network endpoint (npr. "https://api.blockcypher.com/v1/btc/test3")
     */
    private String determineNetworkEndpoint(String network) {
        if (network == null || network.trim().isEmpty()) {
            log.warn("Network not specified, defaulting to testnet");
            return config.getBitcoinApiUrl(); // Default je testnet
        }
        
        String networkLower = network.toLowerCase();
        if (networkLower.contains("test") || networkLower.equals("testnet")) {
            return config.getBitcoinApiUrl(); // Već je testnet u config.getBitcoinApiUrl()
        } else if (networkLower.contains("main") || networkLower.equals("mainnet")) {
            // Za mainnet, menjamo URL
            return "https://api.blockcypher.com/v1/btc/main";
        } else {
            log.warn("Unknown network: {}, defaulting to testnet", network);
            return config.getBitcoinApiUrl();
        }
    }

    /**
     * Proverava status paymenta na blockchain-u koristeći BlockCypher API
     * 
     * @param bitcoinAddress Bitcoin adresa za proveru (merchant adresa)
     * @param expectedAmount Očekivani iznos u BTC
     * @param requiredConfirmations Koliko potvrda je potrebno
     * @param existingTransactionHash Ako transakcija već ima hash, proveri samo tu transakciju (null ako nema)
     * @param transactionCreatedAt Timestamp kada je naša transakcija kreirana (da proverimo da li je blockchain transakcija stigla posle)
     * @return PaymentStatusCheckResult sa informacijama o statusu
     */
    public PaymentStatusCheckResult checkPaymentStatus(String bitcoinAddress, BigDecimal expectedAmount, 
                                                       Integer requiredConfirmations, String existingTransactionHash, 
                                                       java.time.LocalDateTime transactionCreatedAt) {
        // Sanitize address - remove whitespace and validate
        if (bitcoinAddress == null || bitcoinAddress.trim().isEmpty()) {
            log.error("Bitcoin address is null or empty");
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Bitcoin address is null or empty")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
        
        // Trim whitespace and validate format
        bitcoinAddress = bitcoinAddress.trim();
        
        // Basic validation: Bitcoin testnet addresses should be 26-35 characters and start with 'm', 'n', or '2'
        if (bitcoinAddress.length() < 26 || bitcoinAddress.length() > 35) {
            log.error("Invalid Bitcoin address length: {} (expected 26-35 characters)", bitcoinAddress.length());
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Invalid Bitcoin address length: " + bitcoinAddress.length())
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
        
        log.info("Checking payment status for address: {}, expected amount: {} BTC, required confirmations: {}", 
                bitcoinAddress, expectedAmount, requiredConfirmations);
        
        try {
            // Pozovi BlockCypher API da dobiješ informacije o adresi SA TRANSAKCIJAMA
            // GET https://api.blockcypher.com/v1/btc/test3/addrs/{address}/full?limit=50
            // Koristimo /full endpoint da dobijemo sve transakcije
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBitcoinApiUrl())
                    .build();
            
            BlockCypherAddressResponse addressResponse = restClient.get()
                    .uri("/addrs/{address}/full?limit=50", bitcoinAddress)
                    .retrieve()
                    .body(BlockCypherAddressResponse.class);
            
            if (addressResponse == null) {
                log.warn("BlockCypher API returned null response for address: {}", bitcoinAddress);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("BlockCypher API returned null response")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
            
            log.info("Address balance: {} BTC, Total received: {} BTC, Transactions: {}", 
                    addressResponse.getFinalBalanceInBTC(), 
                    addressResponse.getBalanceInBTC(),
                    addressResponse.getNTx());
            
            // Proveri da li postoji transakcija sa očekivanim iznosom
            List<BlockCypherTransaction> transactions = addressResponse.getTransactions();
            
            // Ako već imamo transaction hash, proveri direktno preko hash-a (brže i pouzdanije)
            if (existingTransactionHash != null && !existingTransactionHash.trim().isEmpty()) {
                log.info("Checking existing transaction hash directly: {}", existingTransactionHash);
                
                // Prvo probaj da nađeš u listi transakcija sa adrese
                if (transactions != null && !transactions.isEmpty()) {
                    for (BlockCypherTransaction tx : transactions) {
                        if (existingTransactionHash.equals(tx.getHash())) {
                            // Pronađena je naša transakcija po hash-u!
                            return processFoundTransaction(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
                        }
                    }
                }
                
                // Ako nije pronađena u listi, proveri direktno preko transaction hash endpoint-a
                log.info("Transaction hash not found in address transactions, checking directly via /txs endpoint");
                return checkTransactionByHash(existingTransactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
            }
            
            // Ako nemamo hash i nema transakcija na adresi, možda BlockCypher još nije propagirao
            // Probaj Blockstream API kao fallback (brže indeksiranje)
            if (transactions == null || transactions.isEmpty()) {
                log.info("No transactions found on BlockCypher API for address: {}. Trying Blockstream API as fallback...", bitcoinAddress);
                
                // Ako imamo hash, probaj direktno preko Blockstream-a
                if (existingTransactionHash != null && !existingTransactionHash.trim().isEmpty()) {
                    log.info("Checking transaction hash via Blockstream API: {}", existingTransactionHash);
                    return checkTransactionByHashViaBlockstream(existingTransactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
                }
                
                // Ako nemamo hash, probaj da nađeš transakcije preko Blockstream API-ja
                return checkPaymentStatusViaBlockstream(bitcoinAddress, expectedAmount, requiredConfirmations, existingTransactionHash, transactionCreatedAt);
            }
            
            // Ako nemamo hash, tražimo novu transakciju
            // Proveravamo da li je transakcija stigla POSLE kreiranja naše transakcije
            log.info("Searching for new transaction matching amount {} BTC", expectedAmount);
            
            // Pronađi transakciju koja odgovara očekivanom iznosu
            // Proveravamo output-e transakcija koje su poslate na našu adresu
            for (BlockCypherTransaction tx : transactions) {
                if (tx.getOutputs() == null) {
                    continue;
                }
                
                // Proveri timestamp - transakcija mora biti poslata POSLE kreiranja naše transakcije
                if (transactionCreatedAt != null && tx.getReceived() != null) {
                    try {
                        LocalDateTime txReceived = LocalDateTime.parse(tx.getReceived(), DateTimeFormatter.ISO_DATE_TIME);
                        if (txReceived.isBefore(transactionCreatedAt)) {
                            log.debug("Skipping transaction {} - received before our transaction was created", tx.getHash());
                            continue; // Ova transakcija je stigla pre nego što je naša kreirana
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing transaction timestamp: {}", e.getMessage());
                        // Nastavi sa proverom ako ne možemo da parsirajemo timestamp
                    }
                }
                
                // Proveri sve output-e u transakciji
                for (BlockCypherOutput output : tx.getOutputs()) {
                    if (output.getAddresses() == null || !output.getAddresses().contains(bitcoinAddress)) {
                        continue; // Ovaj output nije za našu adresu
                    }
                    
                    BigDecimal receivedAmount = output.getValueInBTC();
                    
                    // Proveri da li iznos odgovara (sa tolerancijom od 0.00000001 BTC zbog rounding grešaka)
                    BigDecimal difference = receivedAmount.subtract(expectedAmount).abs();
                    boolean amountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;
                    
                    if (amountMatches) {
                        // Pronađena je transakcija sa očekivanim iznosom!
                        log.info("Found matching transaction! Hash: {}, Amount: {} BTC", tx.getHash(), receivedAmount);
                        return processFoundTransaction(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
                    }
                }
            }
            
            // Nije pronađena transakcija sa očekivanim iznosom
            log.info("No transaction found with expected amount {} BTC for address: {}", expectedAmount, bitcoinAddress);
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("No transaction found with expected amount")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
            
        } catch (Exception e) {
            log.error("Error checking payment status via BlockCypher for address: {}. Trying Blockstream API as fallback...", bitcoinAddress, e);
            
            // Ako BlockCypher baci exception, probaj Blockstream API kao fallback
            try {
                return checkPaymentStatusViaBlockstream(bitcoinAddress, expectedAmount, requiredConfirmations, existingTransactionHash, transactionCreatedAt);
            } catch (Exception blockstreamException) {
                log.error("Both BlockCypher and Blockstream API failed for address: {}", bitcoinAddress, blockstreamException);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Error checking payment status: " + e.getMessage() + " | Blockstream fallback also failed: " + blockstreamException.getMessage())
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
        }
    }

    /**
     * Proverava transaction direktno preko hash-a (kada adresa ne vrati transakcije)
     * GET /v1/btc/test3/txs/{hash}
     */
    private PaymentStatusCheckResult checkTransactionByHash(String transactionHash, String bitcoinAddress,
                                                           BigDecimal expectedAmount, Integer requiredConfirmations) {
        log.info("Checking transaction directly by hash: {}", transactionHash);
        
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBitcoinApiUrl())
                    .build();
            
            // Pozovi BlockCypher API da dobiješ informacije o transakciji direktno
            BlockCypherTransaction tx = restClient.get()
                    .uri("/txs/{hash}", transactionHash)
                    .retrieve()
                    .body(BlockCypherTransaction.class);
            
            if (tx == null) {
                log.warn("BlockCypher API returned null response for transaction hash: {}. Trying Blockstream API as fallback...", transactionHash);
                // Fallback na Blockstream API
                return checkTransactionByHashViaBlockstream(transactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
            }
            
            // Proveri da li transakcija šalje na našu adresu
            boolean sendsToOurAddress = false;
            if (tx.getOutputs() != null) {
                for (BlockCypherOutput output : tx.getOutputs()) {
                    if (output.getAddresses() != null && output.getAddresses().contains(bitcoinAddress)) {
                        sendsToOurAddress = true;
                        break;
                    }
                }
            }
            
            if (!sendsToOurAddress) {
                log.warn("Transaction {} does not send to our address: {}", transactionHash, bitcoinAddress);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction does not send to our address")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
            
            // Pronađena je transakcija koja šalje na našu adresu!
            log.info("Transaction found directly by hash: {}", transactionHash);
            return processFoundTransaction(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
            
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // BlockCypher ne vidi transakciju (404) - probaj Blockstream API kao fallback
            log.warn("BlockCypher API returned 404 for transaction hash: {}. Trying Blockstream API as fallback...", transactionHash);
            return checkTransactionByHashViaBlockstream(transactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
        } catch (Exception e) {
            log.error("Error checking transaction by hash via BlockCypher: {}. Trying Blockstream API as fallback...", transactionHash, e);
            // Probaj Blockstream API kao fallback
            try {
                return checkTransactionByHashViaBlockstream(transactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
            } catch (Exception blockstreamException) {
                log.error("Both BlockCypher and Blockstream API failed for transaction hash: {}", transactionHash, blockstreamException);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Error checking transaction hash: " + e.getMessage() + " | Blockstream fallback also failed: " + blockstreamException.getMessage())
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
        }
    }
    
    /**
     * Procesira pronađenu transakciju i vraća PaymentStatusCheckResult
     */
    private PaymentStatusCheckResult processFoundTransaction(BlockCypherTransaction tx, String bitcoinAddress, 
                                                             BigDecimal expectedAmount, Integer requiredConfirmations) {
        // Pronađi output za našu adresu
        BigDecimal receivedAmount = BigDecimal.ZERO;
        for (BlockCypherOutput output : tx.getOutputs()) {
            if (output.getAddresses() != null && output.getAddresses().contains(bitcoinAddress)) {
                receivedAmount = receivedAmount.add(output.getValueInBTC());
            }
        }
        
        Integer confirmations = tx.getConfirmations() != null ? tx.getConfirmations() : 0;
        boolean confirmed = confirmations >= requiredConfirmations;
        
        // Parsiraj timestamp ako postoji
        LocalDateTime receivedAt = null;
        LocalDateTime confirmedAt = null;
        
        try {
            if (tx.getReceived() != null) {
                receivedAt = LocalDateTime.parse(tx.getReceived(), DateTimeFormatter.ISO_DATE_TIME);
            }
            if (tx.getConfirmed() != null) {
                confirmedAt = LocalDateTime.parse(tx.getConfirmed(), DateTimeFormatter.ISO_DATE_TIME);
            }
        } catch (Exception e) {
            log.warn("Error parsing timestamp: {}", e.getMessage());
        }
        
        log.info("Payment found! Hash: {}, Amount: {} BTC, Confirmations: {}/{}", 
                tx.getHash(), receivedAmount, confirmations, requiredConfirmations);
        
        return PaymentStatusCheckResult.builder()
                .paymentFound(true)
                .transactionHash(tx.getHash())
                .confirmations(confirmations)
                .confirmed(confirmed)
                .receivedAmount(receivedAmount)
                .expectedAmount(expectedAmount)
                .amountMatches(true)
                .receivedAt(receivedAt)
                .confirmedAt(confirmedAt)
                .message(confirmed ? 
                        String.format("Payment confirmed with %d confirmations", confirmations) :
                        String.format("Payment found but not confirmed yet (%d/%d confirmations)", 
                                confirmations, requiredConfirmations))
                .build();
    }

    /**
     * Proverava status paymenta na blockchain-u koristeći Blockstream API (fallback kada BlockCypher ne vidi)
     * 
     * @param bitcoinAddress Bitcoin adresa za proveru
     * @param expectedAmount Očekivani iznos u BTC
     * @param requiredConfirmations Koliko potvrda je potrebno
     * @param existingTransactionHash Ako transakcija već ima hash, proveri samo tu transakciju
     * @param transactionCreatedAt Timestamp kada je naša transakcija kreirana
     * @return PaymentStatusCheckResult sa informacijama o statusu
     */
    private PaymentStatusCheckResult checkPaymentStatusViaBlockstream(String bitcoinAddress, BigDecimal expectedAmount,
                                                                      Integer requiredConfirmations, String existingTransactionHash,
                                                                      LocalDateTime transactionCreatedAt) {
        // Sanitize address
        if (bitcoinAddress == null || bitcoinAddress.trim().isEmpty()) {
            log.error("Bitcoin address is null or empty for Blockstream check");
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Bitcoin address is null or empty")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
        
        bitcoinAddress = bitcoinAddress.trim();
        
        log.info("Checking payment status via Blockstream API for address: {}, expected amount: {} BTC", 
                bitcoinAddress, expectedAmount);
        
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBlockstreamApiUrl())
                    .build();
            
            // Ako imamo hash, proveri direktno
            if (existingTransactionHash != null && !existingTransactionHash.trim().isEmpty()) {
                return checkTransactionByHashViaBlockstream(existingTransactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
            }
            
            // Ako nemamo hash, probaj da nađeš transakcije na adresi
            // GET https://blockstream.info/testnet/api/address/{address}/txs
            List<Map<String, Object>> transactionsRaw = restClient.get()
                    .uri("/address/{address}/txs", bitcoinAddress)
                    .retrieve()
                    .body(List.class);
            
            if (transactionsRaw == null || transactionsRaw.isEmpty()) {
                log.info("No transactions found on Blockstream API for address: {}", bitcoinAddress);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("No transactions found on Blockstream API")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
            
            // Pronađi transakciju koja odgovara očekivanom iznosu
            log.info("Searching for transaction matching amount {} BTC in {} transactions", expectedAmount, transactionsRaw.size());
            
            for (Map<String, Object> txObj : transactionsRaw) {
                // Konvertuj Map u BlockstreamTransaction
                BlockstreamTransaction tx = objectMapper.convertValue(txObj, BlockstreamTransaction.class);
                
                if (tx.getVout() == null) {
                    continue;
                }
                
                // Proveri sve output-e u transakciji
                for (BlockstreamOutput output : tx.getVout()) {
                    if (!bitcoinAddress.equals(output.getScriptpubkeyAddress())) {
                        continue; // Ovaj output nije za našu adresu
                    }
                    
                    BigDecimal receivedAmount = output.getValueInBTC();
                    
                    // Proveri da li iznos odgovara
                    BigDecimal difference = receivedAmount.subtract(expectedAmount).abs();
                    boolean amountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;
                    
                    if (amountMatches) {
                        // Pronađena je transakcija sa očekivanim iznosom!
                        log.info("Found matching transaction via Blockstream! Hash: {}, Amount: {} BTC", tx.getTxid(), receivedAmount);
                        return processFoundTransactionViaBlockstream(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
                    }
                }
            }
            
            // Nije pronađena transakcija sa očekivanim iznosom
            log.info("No transaction found with expected amount {} BTC for address: {}", expectedAmount, bitcoinAddress);
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("No transaction found with expected amount on Blockstream API")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
            
        } catch (Exception e) {
            log.error("Error checking payment status via Blockstream API for address: {}", bitcoinAddress, e);
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Error checking payment status via Blockstream API: " + e.getMessage())
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
    }

    /**
     * Proverava transaction direktno preko hash-a koristeći Blockstream API
     * GET https://blockstream.info/testnet/api/tx/{txid}
     */
    private PaymentStatusCheckResult checkTransactionByHashViaBlockstream(String transactionHash, String bitcoinAddress,
                                                                         BigDecimal expectedAmount, Integer requiredConfirmations) {
        log.info("Checking transaction directly by hash via Blockstream API: {}", transactionHash);
        
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBlockstreamApiUrl())
                    .build();
            
            // Pozovi Blockstream API da dobiješ informacije o transakciji direktno
            BlockstreamTransaction tx = restClient.get()
                    .uri("/tx/{txid}", transactionHash)
                    .retrieve()
                    .body(BlockstreamTransaction.class);
            
            if (tx == null) {
                log.warn("Blockstream API returned null response for transaction hash: {}", transactionHash);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction hash not found on Blockstream API")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
            
            // Proveri da li transakcija šalje na našu adresu i da li iznos odgovara
            BigDecimal receivedAmount = BigDecimal.ZERO;
            boolean addressFoundInOutputs = false;
            
            if (tx.getVout() != null) {
                for (BlockstreamOutput output : tx.getVout()) {
                    if (bitcoinAddress.equals(output.getScriptpubkeyAddress())) {
                        receivedAmount = receivedAmount.add(output.getValueInBTC());
                        addressFoundInOutputs = true;
                    }
                }
            }
            
            if (!addressFoundInOutputs) {
                log.warn("Transaction {} found by hash, but our address {} not found in its outputs.", transactionHash, bitcoinAddress);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction found by hash, but our address not in outputs")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }
            
            BigDecimal difference = receivedAmount.subtract(expectedAmount).abs();
            boolean amountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;
            
            if (!amountMatches) {
                log.warn("Transaction {} found by hash, but amount mismatch. Expected: {} BTC, Received: {} BTC",
                        transactionHash, expectedAmount, receivedAmount);
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction found by hash, but amount mismatch")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(receivedAmount)
                        .amountMatches(false)
                        .build();
            }
            
            // Ako je sve OK, procesiraj kao pronađenu transakciju
            return processFoundTransactionViaBlockstream(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
            
        } catch (Exception e) {
            log.error("Error checking transaction directly by hash via Blockstream API: {}", transactionHash, e);
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Error checking transaction by hash via Blockstream API: " + e.getMessage())
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
    }

    /**
     * Procesira pronađenu transakciju iz Blockstream API-ja i vraća PaymentStatusCheckResult
     */
    private PaymentStatusCheckResult processFoundTransactionViaBlockstream(BlockstreamTransaction tx, String bitcoinAddress,
                                                                          BigDecimal expectedAmount, Integer requiredConfirmations) {
        // Pronađi output za našu adresu
        BigDecimal receivedAmount = BigDecimal.ZERO;
        for (BlockstreamOutput output : tx.getVout()) {
            if (bitcoinAddress.equals(output.getScriptpubkeyAddress())) {
                receivedAmount = receivedAmount.add(output.getValueInBTC());
            }
        }
        
        // Blockstream vraća status sa block_height
        Integer confirmations = 0;
        boolean confirmed = false;
        
        if (tx.getStatus() != null && tx.getStatus().getConfirmed() != null && tx.getStatus().getConfirmed()) {
            confirmed = true;
            // Blockstream ne vraća direktno confirmations, moramo izračunati
            // Dobijamo trenutni block height i oduzimamo block_height transakcije
            try {
                Integer txBlockHeight = tx.getStatus().getBlockHeight();
                if (txBlockHeight != null) {
                    // Dobijamo trenutni block height
                    RestClient restClient = restClientBuilder
                            .baseUrl(config.getBlockstreamApiUrl())
                            .build();
                    
                    // GET /blocks/tip/height - vraća trenutni block height
                    Integer currentBlockHeight = restClient.get()
                            .uri("/blocks/tip/height")
                            .retrieve()
                            .body(Integer.class);
                    
                    if (currentBlockHeight != null && txBlockHeight != null) {
                        confirmations = currentBlockHeight - txBlockHeight + 1; // +1 jer je transakcija u tom bloku
                        if (confirmations < 0) {
                            confirmations = 0;
                        }
                    } else {
                        // Fallback: ako je confirmed, ima bar 1 confirmation
                        confirmations = 1;
                    }
                }
            } catch (Exception e) {
                log.warn("Error calculating confirmations for transaction {}: {}. Using fallback.", tx.getTxid(), e.getMessage());
                // Fallback: ako je confirmed, ima bar 1 confirmation
                confirmations = 1;
            }
        }
        
        // Parsiraj timestamp ako postoji
        LocalDateTime receivedAt = null;
        LocalDateTime confirmedAt = null;
        
        if (tx.getStatus() != null && tx.getStatus().getBlockTime() != null) {
            try {
                confirmedAt = LocalDateTime.ofEpochSecond(tx.getStatus().getBlockTime(), 0, 
                        java.time.ZoneOffset.UTC);
                receivedAt = confirmedAt; // Blockstream ne vraća received timestamp odvojeno
            } catch (Exception e) {
                log.warn("Error parsing timestamp for transaction {}: {}", tx.getTxid(), e.getMessage());
            }
        }
        
        log.info("Payment found via Blockstream! Hash: {}, Received Amount: {} BTC, Expected Amount: {} BTC, Confirmations: {}/{}",
                tx.getTxid(), receivedAmount, expectedAmount, confirmations, requiredConfirmations);
        
        return PaymentStatusCheckResult.builder()
                .paymentFound(true)
                .transactionHash(tx.getTxid())
                .confirmations(confirmations)
                .confirmed(confirmed && confirmations >= requiredConfirmations)
                .receivedAmount(receivedAmount)
                .expectedAmount(expectedAmount)
                .amountMatches(true)
                .receivedAt(receivedAt)
                .confirmedAt(confirmedAt)
                .message(confirmed && confirmations >= requiredConfirmations ?
                        String.format("Payment confirmed with %d confirmations (via Blockstream)", confirmations) :
                        String.format("Payment found but not confirmed yet (%d/%d confirmations, via Blockstream)",
                                confirmations, requiredConfirmations))
                .build();
    }
}
