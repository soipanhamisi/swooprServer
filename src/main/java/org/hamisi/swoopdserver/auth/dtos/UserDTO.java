package org.hamisi.swoopdserver.auth.dtos;

import lombok.Getter;
import lombok.Setter;
import org.hamisi.swoopdserver.users.Role;

@Getter
@Setter
public class UserDTO {
   private String fullName;
   private String email;
   private String password;
   private Role role;
}
