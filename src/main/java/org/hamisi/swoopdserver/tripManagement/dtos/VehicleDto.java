package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class VehicleDto {
    private String regNo;
    private String desc;
}
