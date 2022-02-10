package supplimentary;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor

public class StopSpecial {
    int minStops;
    String nearestMetro;
    List<Double> trips;
    boolean isMetro;
    double distMetro;
}
