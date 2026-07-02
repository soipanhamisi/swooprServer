package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Getter
@Service
public class VehicleDto {
    private String regNo;
    private String desc;
}
