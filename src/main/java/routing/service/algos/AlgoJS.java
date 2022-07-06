package routing.service.algos;

import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import routing.entity.WayPoint;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;
import routing.service.Matrix;

import java.util.*;

public abstract class AlgoJS extends Algo
{
    protected static VehicleRoutingTransportCosts costMatrix;
    protected static VehicleType vehicleTypeMoscowBus;

    public static int itCount=0;

    public abstract Result Calculate(
            List<WayPoint> wayPointList,
            Map<WayPoint, MatrixLineMap> matrixIn,
            AlgoParams params,
            CellStopPattern cellStopPattern);


    protected static void InitJsprit()
    {
        //define a matrix-builder building a NON-symmetric matrix
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix
                .Builder.newInstance(false);

        // ---- transfer our matrix to jsprit matrix ----
        for(int jj=0;jj<wayPoints.size();jj++)
            for(int kk=0;kk<wayPoints.size();kk++)
                if(jj!=kk)
                {
                    costMatrixBuilder.addTransportDistance(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.DistanceBetweenMap(wayPoints.get(jj),wayPoints.get(kk),matrix));
                    costMatrixBuilder.addTransportTime(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.TimeBetweenMap(wayPoints.get(jj),wayPoints.get(kk),matrix));
                }
                else
                {
                    costMatrixBuilder.addTransportDistance(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Double.POSITIVE_INFINITY);
                    costMatrixBuilder.addTransportTime(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Double.POSITIVE_INFINITY);
                }
        costMatrix = costMatrixBuilder.build();

        /*
         * get a vehicle type-builder and build a type
         */
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("Moscow Bus Type")
                .addCapacityDimension(0, params.getCapacity())
                .setCostPerDistance(0) // for custom objective function
                .setCostPerTransportTime(1); // for custom objective function
        vehicleTypeMoscowBus = vehicleTypeBuilder.build();
    }

}
