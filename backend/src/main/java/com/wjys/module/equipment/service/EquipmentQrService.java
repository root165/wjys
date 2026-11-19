package com.wjys.module.equipment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class EquipmentQrService {
    private final EquipmentService equipmentService;
    private final String qrSecret;

    public EquipmentQrService(EquipmentService equipmentService, @Value("${wjys.security.qr-secret}") String qrSecret) {
        this.equipmentService = equipmentService;
        this.qrSecret = qrSecret;
    }

    public byte[] generate(Long id) {
        String code = equipmentService.getActive(id).getEquipCode();
        String payload = "WJYS:EQ:" + code + ":" + signature(code);
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360, Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("二维码生成失败", exception);
        }
    }

    public String verify(String payload) {
        String[] parts = payload == null ? new String[0] : payload.split(":");
        if (parts.length != 4 || !"WJYS".equals(parts[0]) || !"EQ".equals(parts[1]) || !MessageDigest.isEqual(parts[3].getBytes(StandardCharsets.UTF_8), signature(parts[2]).getBytes(StandardCharsets.UTF_8))) throw new IllegalArgumentException("二维码签名无效");
        return parts[2];
    }

    private String signature(String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(code.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception exception) { throw new IllegalStateException("二维码签名失败", exception); }
    }
}
