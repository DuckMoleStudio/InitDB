import entity.Version;

import static loader.Compare.compareCellsByFull;
import static loader.Compare.compareCellsHS;
import static service.VersionService.getVersionById;

public class Comparation {
    public static void main(String[] args) {
        Version v1 = getVersionById(3);
        Version v2 = getVersionById(4);

        //compareCellsByFull(v1,v2);
        compareCellsHS(3,4);
    }
}
