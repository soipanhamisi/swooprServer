package org.hamisi.swoopdserver.auth.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailOtpDTO {
    private String email;
    private int otp;
}
