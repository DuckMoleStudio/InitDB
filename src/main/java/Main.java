
import entity.Route;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import static service.RouteService.*;

public class Main {
    public static void main(String[] args) {
// Create entity
        final Route route = new Route();

        // Get significant fields

        route.setId(103);
        route.setShortName("30");
        route.setLongName("Third");



        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
        //Point point = gf.createPoint(new Coordinate(22,33));

        LineString[] ls = new LineString[2];
        ls[0] = gf.createLineString(new Coordinate[]{new Coordinate(37.45,55.68),new Coordinate(37.4,55.75)});
        ls[1] = gf.createLineString(new Coordinate[]{new Coordinate(37.58,55.78),new Coordinate(37.4,55.75)});
        MultiLineString ml = gf.createMultiLineString(ls);

        route.setGeomML(ml);



/*
        try {
            route.setGeomPoint((Point) wktToGeometry("POINT (2 5)"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

 */




        addRoute(route);
        //listRoutes();

        /*
        Route route1=getRouteById(130);
        System.out.println(route1.getLongName());
        System.out.println(route1.getGeomML().toString());




        Route route2=getRouteById(131);
        System.out.println(route2.getLongName());
        System.out.println(route2.getGeomML().toString());

         */

    }

    public static Geometry wktToGeometry(String wellKnownText) throws ParseException {

        return new WKTReader().read(wellKnownText);
    }
}
