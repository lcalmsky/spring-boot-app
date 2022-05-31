package io.lcalmsky.app.infra.mail;

import lombok.*;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EmailMessage {
    private String to;
    private String subject;
    private String message;
}
