package com.example.service;

import com.example.config.ConfigProperties;
import com.example.dto.BlockstreamOutput;
import com.example.dto.BlockstreamTransaction;
import com.example.dto.CryptoConfig;
import com.example.dto.PaymentStatusCheckResult;
import com.example.model.AddressIndex;
import com.example.repository.AddressIndexRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.params.MainNetParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitcoinClient {

    private final ObjectMapper objectMapper;
    private final ConfigProperties config;
    private final RestClient.Builder restClientBuilder;
    private final AddressIndexRepository addressIndexRepository;


    public CryptoConfig parseConfig(String configJson) {
        try {
            return objectMapper.readValue(configJson, CryptoConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid crypto configuration: " + e.getMessage(), e);
        }
    }


    public BigDecimal convertToBitcoin(BigDecimal fiatAmount, String fiatCurrency) {

        try {

            RestClient restClient = restClientBuilder
                    .baseUrl("https://api.coingecko.com/api/v3")
                    .build();
            
            String currencyLower = fiatCurrency.toLowerCase();
            Map<String, Object> response = restClient.get()
                    .uri("/simple/price?ids=bitcoin&vs_currencies={currency}", currencyLower)
                    .retrieve()
                    .body(Map.class);
            
            if (response == null || !response.containsKey("bitcoin")) {
                throw new RuntimeException("Failed to get Bitcoin price from CoinGecko API");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> bitcoinData = (Map<String, Object>) response.get("bitcoin");
            
            if (!bitcoinData.containsKey(currencyLower)) {
                throw new RuntimeException("Currency " + fiatCurrency + " not supported by CoinGecko API");
            }

            Object priceObj = bitcoinData.get(currencyLower);
            BigDecimal bitcoinPrice;
            
            if (priceObj instanceof Number) {
                bitcoinPrice = BigDecimal.valueOf(((Number) priceObj).doubleValue());
            } else if (priceObj instanceof String) {
                bitcoinPrice = new BigDecimal((String) priceObj);
            } else {
                throw new RuntimeException("Invalid price format from CoinGecko API");
            }

            
            // ovde konvertujem iznos u eurima kroz iznos u eurima jednog bitkoina za dobijem iznos u bitkoinu
            BigDecimal bitcoinAmount = fiatAmount.divide(bitcoinPrice, 8, RoundingMode.HALF_UP);
            
            return bitcoinAmount;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert " + fiatAmount + " " + fiatCurrency + " to BTC: " + e.getMessage(), e);
        }
    }


    public AddressGenerationResult generateBitcoinAddressWithIndex(CryptoConfig cryptoConfig) {
        if (cryptoConfig == null) {
            throw new IllegalArgumentException("CryptoConfig cannot be null");
        }

        if (cryptoConfig.getXpub() != null && !cryptoConfig.getXpub().trim().isEmpty()) {
            try {
                return generateAddressFromXpub(cryptoConfig);
            } catch (Exception e) {
                if (cryptoConfig.getWalletAddress() != null && !cryptoConfig.getWalletAddress().trim().isEmpty()) {
                    return new AddressGenerationResult(cryptoConfig.getWalletAddress(), null);
                }
                throw new RuntimeException("Failed to generate address from xpub and no fallback walletAddress provided: " + e.getMessage(), e);
            }
        }

        // Prioritet 2: Statička walletAddress (backward compatibility)
        String walletAddress = cryptoConfig.getWalletAddress();
        if (walletAddress != null && !walletAddress.trim().isEmpty()) {
            return new AddressGenerationResult(walletAddress, null);
        }

        throw new IllegalArgumentException(
                "Either provide 'xpub' for HD wallet address generation, " +
                "or provide 'walletAddress' for static address (backward compatibility).");
    }


    public String generateBitcoinAddress(CryptoConfig cryptoConfig) {
        return generateBitcoinAddressWithIndex(cryptoConfig).address;
    }


    @Transactional
    public AddressGenerationResult generateAddressFromXpub(CryptoConfig cryptoConfig) {
        try {
            String xpubString = cryptoConfig.getXpub().trim();

            NetworkParameters params = determineNetworkParams(cryptoConfig.getNetwork(), xpubString);

            DeterministicKey masterPublicKey = DeterministicKey.deserializeB58(xpubString, params);

            AddressIndex addressIndex = addressIndexRepository.findById(1L)
                    .orElseGet(() -> {
                        AddressIndex newIndex = new AddressIndex();
                        newIndex.setId(1L);
                        newIndex.setLastUsedIndex(0);
                        return addressIndexRepository.save(newIndex);
                    });


            int nextIndex = addressIndex.getLastUsedIndex() + 1;

            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(masterPublicKey, 0);
            DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(accountKey, nextIndex);

            Address address;
            try {
                address = SegwitAddress.fromKey(params, addressKey);
            } catch (Exception e) {
                address = LegacyAddress.fromKey(params, addressKey);
            }

            addressIndex.setLastUsedIndex(nextIndex);
            addressIndexRepository.save(addressIndex);
            return new AddressGenerationResult(address.toString(), nextIndex);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate address from xpub: " + e.getMessage(), e);
        }
    }


    private NetworkParameters determineNetworkParams(String network, String xpub) {

        if (network != null && !network.trim().isEmpty()) {
            String networkLower = network.toLowerCase();
            if (networkLower.contains("test") || networkLower.equals("testnet")) {
                return TestNet3Params.get();
            } else if (networkLower.contains("main") || networkLower.equals("mainnet")) {
                return MainNetParams.get();
            }
        }

        if (xpub != null && !xpub.trim().isEmpty()) {
            String xpubLower = xpub.trim().toLowerCase();
            if (xpubLower.startsWith("vpub") || xpubLower.startsWith("tpub")) {
                return TestNet3Params.get();
            }
            if (xpubLower.startsWith("xpub") || xpubLower.startsWith("zpub")) {
                return MainNetParams.get();
            }
        }

        return TestNet3Params.get();
    }

    public static class AddressGenerationResult {
        public final String address;
        public final Integer derivationIndex;

        public AddressGenerationResult(String address, Integer derivationIndex) {
            this.address = address;
            this.derivationIndex = derivationIndex;
        }
    }



    public PaymentStatusCheckResult checkPaymentStatus(String bitcoinAddress, BigDecimal expectedAmount, 
                                                       Integer requiredConfirmations, String existingTransactionHash, 
                                                       java.time.LocalDateTime transactionCreatedAt) {


        if (bitcoinAddress == null || bitcoinAddress.trim().isEmpty()) {
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Bitcoin address is null or empty")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }

        bitcoinAddress = bitcoinAddress.trim();
        if (bitcoinAddress.length() < 26 || bitcoinAddress.length() > 62) {
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Invalid Bitcoin address length: " + bitcoinAddress.length() + " (expected 26-62 characters)")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }

        return checkPaymentStatusViaBlockstream(bitcoinAddress, expectedAmount, requiredConfirmations, existingTransactionHash, transactionCreatedAt);
    }


    private PaymentStatusCheckResult checkPaymentStatusViaBlockstream(String bitcoinAddress, BigDecimal expectedAmount,
                                                                      Integer requiredConfirmations, String existingTransactionHash,
                                                                      LocalDateTime transactionCreatedAt) {

        if (bitcoinAddress == null || bitcoinAddress.trim().isEmpty()) {
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Bitcoin address is null or empty")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
        
        bitcoinAddress = bitcoinAddress.trim();
        

        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBlockstreamApiUrl())
                    .build();

            // Ako imamo hash, provjeri direktno
            if (existingTransactionHash != null && !existingTransactionHash.trim().isEmpty()) {
                return checkTransactionByHashViaBlockstream(existingTransactionHash, bitcoinAddress, expectedAmount, requiredConfirmations);
            }
            
            // ako nemamo hash, probaj da nađeš transakcije na adresi
            String apiUrl = config.getBlockstreamApiUrl() + "/address/" + bitcoinAddress + "/txs";

            List<Map<String, Object>> transactionsRaw = restClient.get()
                    .uri("/address/{address}/txs", bitcoinAddress)
                    .retrieve()
                    .body(List.class);
            
            if (transactionsRaw == null || transactionsRaw.isEmpty()) {
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("No transactions found on Blockstream API")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }



            BigDecimal maxReceivedAmount = BigDecimal.ZERO;
            String foundTransactionHash = null;

            for (Map<String, Object> txObj : transactionsRaw) {
                BlockstreamTransaction tx = objectMapper.convertValue(txObj, BlockstreamTransaction.class);

                if (tx.getVout() == null) {
                    continue;
                }

                for (BlockstreamOutput output : tx.getVout()) {

                    if (!bitcoinAddress.equals(output.getScriptpubkeyAddress())) {
                        continue;
                    }
                    
                    BigDecimal receivedAmount = output.getValueInBTC();

                    BigDecimal difference = receivedAmount.subtract(expectedAmount).abs();
                    boolean amountMatches = difference.compareTo(new BigDecimal("0.00000001")) <= 0;

                    if (amountMatches) {
                        return processFoundTransactionViaBlockstream(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
                    } else {
                        if (receivedAmount.compareTo(maxReceivedAmount) > 0) {
                            maxReceivedAmount = receivedAmount;
                            foundTransactionHash = tx.getTxid();
                        }
                    }
                }
            }

            if (maxReceivedAmount.compareTo(BigDecimal.ZERO) > 0) {
                return PaymentStatusCheckResult.builder()
                        .paymentFound(true) // Payment je pronađen, ali amount ne odgovara
                        .message(String.format("Transaction found but amount mismatch: received %s BTC, expected %s BTC",
                                maxReceivedAmount, expectedAmount))
                        .expectedAmount(expectedAmount)
                        .receivedAmount(maxReceivedAmount)
                        .amountMatches(false)
                        .transactionHash(foundTransactionHash)
                        .build();
            }
            
            // Nije pronađena transakcija sa očekivanim iznosom
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("No transaction found with expected amount on Blockstream API")
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
            
        } catch (Exception e) {
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Error checking payment status via Blockstream API: " + e.getMessage())
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
    }

    private PaymentStatusCheckResult checkTransactionByHashViaBlockstream(String transactionHash, String bitcoinAddress,
                                                                         BigDecimal expectedAmount, Integer requiredConfirmations) {

        
        try {
            RestClient restClient = restClientBuilder
                    .baseUrl(config.getBlockstreamApiUrl())
                    .build();

            BlockstreamTransaction tx = restClient.get()
                    .uri("/tx/{txid}", transactionHash)
                    .retrieve()
                    .body(BlockstreamTransaction.class);
            
            if (tx == null) {
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction hash not found on Blockstream API")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(BigDecimal.ZERO)
                        .amountMatches(false)
                        .build();
            }

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
                return PaymentStatusCheckResult.builder()
                        .paymentFound(false)
                        .message("Transaction found by hash, but amount mismatch")
                        .expectedAmount(expectedAmount)
                        .receivedAmount(receivedAmount)
                        .amountMatches(false)
                        .build();
            }

            return processFoundTransactionViaBlockstream(tx, bitcoinAddress, expectedAmount, requiredConfirmations);
            
        } catch (Exception e) {
            return PaymentStatusCheckResult.builder()
                    .paymentFound(false)
                    .message("Error checking transaction by hash via Blockstream API: " + e.getMessage())
                    .expectedAmount(expectedAmount)
                    .receivedAmount(BigDecimal.ZERO)
                    .amountMatches(false)
                    .build();
        }
    }


    private PaymentStatusCheckResult processFoundTransactionViaBlockstream(BlockstreamTransaction tx, String bitcoinAddress,
                                                                          BigDecimal expectedAmount, Integer requiredConfirmations) {
        BigDecimal receivedAmount = BigDecimal.ZERO;
        for (BlockstreamOutput output : tx.getVout()) {
            if (bitcoinAddress.equals(output.getScriptpubkeyAddress())) {
                receivedAmount = receivedAmount.add(output.getValueInBTC());
            }
        }

        Integer confirmations = 0;
        boolean confirmed = false;
        
        if (tx.getStatus() != null && tx.getStatus().getConfirmed() != null && tx.getStatus().getConfirmed()) {
            confirmed = true;
            try {
                Integer txBlockHeight = tx.getStatus().getBlockHeight();
                if (txBlockHeight != null) {

                    RestClient restClient = restClientBuilder
                            .baseUrl(config.getBlockstreamApiUrl())
                            .build();

                    Integer currentBlockHeight = restClient.get()
                            .uri("/blocks/tip/height")
                            .retrieve()
                            .body(Integer.class);
                    
                    if (currentBlockHeight != null && txBlockHeight != null) {
                        confirmations = currentBlockHeight - txBlockHeight + 1;
                        if (confirmations < 0) {
                            confirmations = 0;
                        }
                    } else {
                        confirmations = 1;
                    }
                }
            } catch (Exception e) {
                confirmations = 1;
            }
        }

        LocalDateTime receivedAt = null;
        LocalDateTime confirmedAt = null;
        
        if (tx.getStatus() != null && tx.getStatus().getBlockTime() != null) {
            try {
                confirmedAt = LocalDateTime.ofEpochSecond(tx.getStatus().getBlockTime(), 0, 
                        java.time.ZoneOffset.UTC);
                receivedAt = confirmedAt;
            } catch (Exception e) {
                log.warn("Error parsing timestamp for transaction {}: {}", tx.getTxid(), e.getMessage());
            }
        }

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
