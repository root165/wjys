package com.wjys.module.system.vo;

public record TokenPairVO(String accessToken, String refreshToken, long accessExpiresIn, long refreshExpiresIn) {
}
