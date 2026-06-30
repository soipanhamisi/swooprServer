package org.hamisi.swoopdserver.auth.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailAuthCredsDTO {
    private String email;
    private int otp;
}
