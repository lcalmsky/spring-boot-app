package io.lcalmsky.app.settings.controller;

import lombok.*;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TagForm {
    private String tagTitle;
}
