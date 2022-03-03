package io.lcalmsky.app.settings.controller;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordForm {
    @Length(min = 8, max = 50)
    private String newPassword;
    @Length(min = 8, max = 50)
    private String newPasswordConfirm;
}
