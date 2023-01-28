package com.tron.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

@Data
@Configuration
@ConfigurationProperties(prefix = "tron.config")
public class TronServiceConfig {
  // trc20合约地址
  private String trc20Address;
  // trc20合约精度
  private BigInteger trc20Decimals;
  // 波场api域名
  private String tronDomainUlr;
  // 主、测试账号
  private String ownerAddress;
  // 主、测试账号 私钥
  private String hexPrivateKey;
  // 是否测试环境
  private Boolean tronDomainOnline;
}
