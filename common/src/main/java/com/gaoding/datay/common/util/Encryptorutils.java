package com.gaoding.datay.common.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

public enum Encryptorutils {
    
    INSTANCE;

    PooledPBEStringEncryptor encryptor = null;

     Encryptorutils (){
         encryptor = new PooledPBEStringEncryptor();
         SimpleStringPBEConfig config = new SimpleStringPBEConfig();
         config.setPassword("fJ$U^H1a^sVqat4c");
         config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
         config.setKeyObtentionIterations("1000");
         config.setPoolSize("1");
         config.setProviderName(null);
         config.setProviderClassName(null);
         config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
         config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
         config.setStringOutputType("base64");
         encryptor.setConfig(config);
    }
    
    public  String decrypt(String c){
        return   encryptor.decrypt(c);
    }

    public static void main(String[] args) {
        
        Encryptorutils.INSTANCE.decrypt("addd");

    }

}
