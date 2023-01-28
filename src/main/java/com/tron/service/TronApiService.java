package com.tron.service;

import com.tron.config.TronServiceConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Bool;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.generated.Uint256;
import org.tron.trident.core.ApiWrapper;
import org.tron.trident.core.exceptions.IllegalException;
import org.tron.trident.core.transaction.TransactionBuilder;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;
import org.tron.trident.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

@Service
@Slf4j
public class TronApiService {

  @Autowired private TronServiceConfig tronServiceConfig;

  public ApiWrapper getApiWrapper(String hexPrivateKey) {
    if (tronServiceConfig.getTronDomainOnline()) {
      return ApiWrapper.ofMainnet(hexPrivateKey, "68aea214-f593-4946-8cb2-c5ecb7e9e67f");
    } else {
      return ApiWrapper.ofShasta(hexPrivateKey);
    }
  }

  /**
   * 查询TRC20 交易记录
   *
   * @param address 账号
   * @param only_to 只查询转入交易
   * @param only_from 只查询转出交易
   * @return
   * @throws IOException
   */
  public String getTransactionsTRC20(String address, boolean only_to, boolean only_from) {
    OkHttpClient client = new OkHttpClient();
    Request request =
        new Request.Builder()
            .url(
                tronServiceConfig.getTronDomainUlr()
                    + "/v1/accounts/"
                    + address
                    + "/transactions/trc20?only_to="
                    + only_to
                    + "&only_from="
                    + only_from
                    + "&limit=10&contract_address="
                    + tronServiceConfig.getTrc20Address())
            .get()
            .addHeader("Accept", "application/json")
            .build();

    try {
      okhttp3.Response response = client.newCall(request).execute();
      return response.body().string();
    } catch (IOException e) {
      log.error(" getTransactionsTRC20 IOException ", e);
    }
    return null;
  }

  /**
   * 转账TRX
   *
   * @param hexPrivateKey
   * @param fromAddress
   * @param toAddress
   * @param amount
   * @return
   * @throws IllegalException
   */
  public String trunsferTRX(String hexPrivateKey, String fromAddress, String toAddress, long amount)
      throws IllegalException {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Response.TransactionExtention transactionExtention =
        client.transfer(fromAddress, toAddress, amount);
    Chain.Transaction transaction = client.signTransaction(transactionExtention);
    String txid = client.broadcastTransaction(transaction);
    client.close();
    return txid;
  }

  /**
   * 创建账号
   *
   * @param hexPrivateKey
   * @param ownerAddress
   * @param accountAddress
   * @return
   * @throws IllegalException
   */
  public String createAccount(String hexPrivateKey, String ownerAddress, String accountAddress)
      throws IllegalException {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Response.TransactionExtention transactionExtention =
        client.createAccount(ownerAddress, accountAddress);
    Chain.Transaction transaction = client.signTransaction(transactionExtention);
    return client.broadcastTransaction(transaction);
  }

  /**
   * 查询交易状态
   *
   * @param txid
   * @return
   * @throws IllegalException
   */
  public String getTransactionStatusById(String txid) throws IllegalException {
    ApiWrapper client = getApiWrapper(tronServiceConfig.getHexPrivateKey());
    Chain.Transaction getTransaction = client.getTransactionById(txid);
    return getTransaction.getRet(0).getContractRet().name();
  }

  /**
   * 查询账户信息
   *
   * @param address
   * @return
   * @throws IllegalException
   */
  public Response.Account getAccount(String hexPrivateKey, String address) {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Response.Account account = client.getAccount(address);
    return account;
  }

  /**
   * 查询账户TRX余额
   *
   * @param address
   * @return
   */
  public BigDecimal getAccountBalance(String hexPrivateKey, String address) {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Response.Account account = client.getAccount(address);

    client.close();
    return new BigDecimal(account.getBalance())
        .divide(new BigDecimal(tronServiceConfig.getTrc20Decimals()));
  }

  public BigDecimal getUSDTBalanceDecimal(String hexPrivateKey, String address) {
    BigInteger balanceOf = getUSDTBalanceOf(hexPrivateKey, address);
    return new BigDecimal(balanceOf).divide(new BigDecimal(tronServiceConfig.getTrc20Decimals()));
  }

  /**
   * 获取USDT余额
   *
   * @param address
   * @return
   */
  public BigInteger getUSDTBalanceOf(String hexPrivateKey, String address) {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Function balanceOf =
        new Function(
            "balanceOf",
            Arrays.asList(new Address(address)),
            Arrays.asList(new TypeReference<Uint256>() {}));
    Response.TransactionExtention extension =
        client.constantCall(address, tronServiceConfig.getTrc20Address(), balanceOf);

    String result = Numeric.toHexString(extension.getConstantResult(0).toByteArray());

    BigInteger value =
        (BigInteger)
            FunctionReturnDecoder.decode(result, balanceOf.getOutputParameters()).get(0).getValue();

    client.close();

    return value;
  }

  /**
   * 转账USDT
   *
   * @param hexPrivateKey
   * @param fromAddress
   * @param toAddress
   * @param amount
   * @return
   */
  public String trunsferUSDT(
      String hexPrivateKey, String fromAddress, String toAddress, BigInteger amount) {
    ApiWrapper client = getApiWrapper(hexPrivateKey);
    Function transfer =
        new Function(
            "transfer",
            Arrays.asList(new Address(toAddress), new Uint256(amount)),
            Arrays.asList(new TypeReference<Bool>() {}));

    TransactionBuilder builder =
        client.triggerCall(fromAddress, tronServiceConfig.getTrc20Address(), transfer);
    builder.setFeeLimit(10000000);

    Chain.Transaction transaction = client.signTransaction(builder.getTransaction());
    String txid = client.broadcastTransaction(transaction);
    client.close();
    return txid;
  }

  /** base58 和 hex 互转 */
  public void format() {
    byte[] rawAddr = Hex.decode("4159d3ad9d126e153b9564417d3a05cf51c1964edf");
    String base58 = Base58Check.bytesToBase58(rawAddr);
    System.out.println(base58);
    byte[] base58Byte = Base58Check.base58ToBytes(base58);
    System.out.println(Hex.toHexString(base58Byte));
  }

  public static void main(String[] args) {
    String base58 = "TQFV9kQbFjr7p1t4vRZKU43m2np28NXUXc";
    System.out.println(base58);
    byte[] base58Byte = Base58Check.base58ToBytes(base58);
    System.out.println(Hex.toHexString(base58Byte));
  }
}
