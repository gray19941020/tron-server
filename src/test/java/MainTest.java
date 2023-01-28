import com.tron.TronServerApplication;
import com.tron.config.TronServiceConfig;
import com.tron.service.TronApiService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.tron.trident.core.key.KeyPair;
import org.tron.trident.crypto.SECP256K1;
import org.tron.trident.utils.Base58Check;

import java.math.BigDecimal;
import java.math.BigInteger;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TronServerApplication.class})
@Slf4j
public class MainTest {
  @Autowired private TronApiService tronApiService;
  @Autowired private TronServiceConfig config;
  public static String toAddress = "TXoDw9PUuBWwp2GmiDvFwdXfja6KREpkyY";

  /** 获取trx,usdt余额 */
  @Test
  public void balanceOf() {
    BigDecimal trxBalanceOf =
        tronApiService.getAccountBalance(config.getHexPrivateKey(), config.getOwnerAddress());
    BigDecimal usdtBalanceOf =
        tronApiService.getUSDTBalanceDecimal(config.getHexPrivateKey(), config.getOwnerAddress());
    log.info("trx 余额:{}", trxBalanceOf);
    log.info("usdt余额:{}", usdtBalanceOf);
  }

  /** 转账TRX */
  @Test
  @SneakyThrows
  public void trunsferTRX() {
    String txid =
        tronApiService.trunsferTRX(
            config.getHexPrivateKey(), config.getOwnerAddress(), toAddress, 1000000L);
    log.info("交易ID:{}", txid);
    Thread.sleep(5000);
    String status = tronApiService.getTransactionStatusById(txid);
    log.info("交易状态:{}", status);
  }

  /** 转账USDT */
  @Test
  @SneakyThrows
  public void trunsferUSDT() {
    String txid =
        tronApiService.trunsferUSDT(
            config.getHexPrivateKey(),
            config.getOwnerAddress(),
            "TKXP6StDTJXfWvbvM3EWjm7y7JXP9LpwkF",
            config.getTrc20Decimals().multiply(new BigInteger("100")));
    log.info("交易ID:{}", txid);
    Thread.sleep(5000);
    String status = tronApiService.getTransactionStatusById(txid);
    log.info("交易状态:{}", status);
  }

  /** 查询TRC20交易记录 */
  @Test
  @SneakyThrows
  public void getTransactionsTRC20() {
    String json = tronApiService.getTransactionsTRC20(config.getOwnerAddress(), false, false);
    log.info("交易记录:{}", json);
  }

  /** 私钥转钱包地址 */
  @Test
  @SneakyThrows
  public void privateKeyToAddress() {
    byte[] rawAddr =
        KeyPair.publicKeyToAddress(
            SECP256K1.PublicKey.create(SECP256K1.PrivateKey.create(config.getHexPrivateKey())));
    String address = Base58Check.bytesToBase58(rawAddr);
    log.info("地址:{}", address);
  }


  /** 创建账号，并激活 */
  @Test
  @SneakyThrows
  public void createWallet() {
    KeyPair keyPair = KeyPair.generate();
    log.info(" address：{}, PrivateKey：{}", keyPair.toBase58CheckAddress(), keyPair.toPrivateKey());
    tronApiService.createAccount(
        keyPair.toPrivateKey(), keyPair.toBase58CheckAddress(), config.getHexPrivateKey());
  }
}
