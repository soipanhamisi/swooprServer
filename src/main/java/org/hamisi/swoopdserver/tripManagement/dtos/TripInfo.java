package org.hamisi.swoopdserver.tripManagement.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripInfo {
    private List<String> carpoolMemberNames;
    private TripData tripData;
}
