package com.test.offchain;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.ScriptUtxoFinders;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import scalus.bloxbean.ScalusTransactionEvaluator;

public class Main {

    BackendService backendService =
            new BFBackendService(Constants.BLOCKFROST_PREPROD_URL, "<BF_KEY>");

    String senderMnemonic = "<your mnemonic>";
    Account sender1 = new Account(Networks.testnet(), senderMnemonic);
    String sender1Addr = sender1.baseAddress();

    PlutusV2Script plutusScript = PlutusV2Script.builder()
            .type("PlutusScriptV2")
            .cborHex("4c4b010000223300214a229401")
            .build();

    private void lockFund() {

        var scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        var tx = new Tx()
                .payToContract(scriptAddress, Amount.ada(10), BigIntPlutusData.of(2001))
                .from(sender1Addr);

        var result = quickTxBuilder
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(System.out::println);

        System.out.println(result);
    }

    private void unlock() throws Exception {
        var scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();

        var utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());
        var protocolParamSupplier = new DefaultProtocolParamsSupplier(backendService.getEpochService());
        var utxo = ScriptUtxoFinders.findFirstByInlineDatum(utxoSupplier, scriptAddress, BigIntPlutusData.of(2001)).get();

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        var tx = new ScriptTx()
                .collectFrom(utxo, ConstrPlutusData.of(0))
                .payToAddress(sender1Addr, Amount.ada(10))
                .attachSpendingValidator(plutusScript);

        var result = quickTxBuilder
                .compose(tx)
                .feePayer(sender1Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withTxEvaluator(new ScalusTransactionEvaluator(protocolParamSupplier.getProtocolParams(), utxoSupplier))
                .withTxInspector(txn -> {
                    System.out.println(txn);
                })
                .completeAndWait(System.out::println);

        System.out.println(result);
    }

    public static void main(String[] args) throws Exception {
//        new Main().lockFund();
        new Main().unlock();
    }
}
