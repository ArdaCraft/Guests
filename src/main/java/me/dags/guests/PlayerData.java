package me.dags.guests;

import com.flowpowered.math.vector.Vector3d;
import java.util.UUID;
import java.util.function.Function;
import org.spongepowered.api.world.Locatable;

/**
 * @author dags <dags@dags.me>
 */
public class PlayerData {

    private static final long CHECK_INTERVAL = 100L;
    static final Function<UUID, PlayerData> CONSTRUCTOR = u -> new PlayerData();

    private long lastMovementCheck = 0L;
    private Vector3d lastPosition = Vector3d.ZERO;

    public void setPosition(Vector3d position) {
        this.lastPosition = position;
    }

    public double getVelocitySquared(Locatable player) {
        long time = System.currentTimeMillis();
        long period = time - lastMovementCheck;
        lastMovementCheck = time;

        if (period < CHECK_INTERVAL) {
            return -1;
        }

        Vector3d position = player.getLocation().getPosition();
        if (lastPosition == Vector3d.ZERO) {
            lastPosition = position;
            return -1;
        }

        double distance2 = position.distanceSquared(lastPosition);
        lastPosition = position;

        if (distance2 < 1) {
            return -1;
        }

        return distance2 / (time * time);
    }
}
