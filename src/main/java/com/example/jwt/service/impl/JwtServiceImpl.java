package com.example.jwt.service.impl;

import com.example.jwt.entity.User;
import com.example.jwt.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String subject;
        if (userDetails instanceof User) {
            subject = String.valueOf(((User) userDetails).getId());
        } else {
            subject = userDetails.getUsername();
        }
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String subject = extractUsername(token);
        String comparisonValue;
        if (userDetails instanceof User) {
            comparisonValue = String.valueOf(((User) userDetails).getId());
        } else {
            comparisonValue = userDetails.getUsername();
        }
        return (subject.equals(comparisonValue)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = null;
        if (secretKey != null && secretKey.matches("^[0-9a-fA-F]+$") && secretKey.length() % 2 == 0) {
            try {
                keyBytes = decodeHex(secretKey);
            } catch (Exception ignored) {}
        }
        if (keyBytes == null) {
            try {
                keyBytes = Decoders.BASE64.decode(secretKey);
            } catch (Exception e) {
                keyBytes = secretKey.getBytes();
            }
        }
        if (keyBytes.length < 32) {
            keyBytes = secretKey.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }
}
