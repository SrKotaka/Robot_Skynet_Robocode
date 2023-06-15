package Skynet;

import robocode.*;
import robocode.util.Utils;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

public class Skynet extends AdvancedRobot {
    private Map<String, EnemyInfo> enemies = new HashMap<>();

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);
        
        while (true) {
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        String enemyName = e.getName();
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();

        // Atualizar informações sobre o inimigo
        if (!enemies.containsKey(enemyName)) {
            enemies.put(enemyName, new EnemyInfo());
        }
        EnemyInfo enemyInfo = enemies.get(enemyName);
        enemyInfo.update(enemyAbsoluteBearing, e.getDistance(), e.getVelocity(), e.getHeadingRadians());

        // Prever a posição futura do inimigo
        double bulletPower = Math.min(3.0, getEnergy());
        double bulletSpeed = Rules.getBulletSpeed(bulletPower);
        long timeToHit = (long) (e.getDistance() / bulletSpeed);

        double futureX = enemyInfo.predictX(timeToHit);
        double futureY = enemyInfo.predictY(timeToHit);

        // Calcular a direção e distância para o inimigo previsto
        double desiredHeading = Math.atan2(futureX - getX(), futureY - getY());
        double turnAmount = Utils.normalRelativeAngle(desiredHeading - getHeadingRadians());

        setTurnRightRadians(turnAmount);
        setAhead(Double.POSITIVE_INFINITY);

        // Atirar na direção prevista do inimigo
        double enemyHeading = enemyInfo.getHeading();
        double enemyVelocity = enemyInfo.getVelocity();
        double absoluteBearing = enemyAbsoluteBearing + Math.asin(enemyVelocity / bulletSpeed * Math.sin(enemyHeading - enemyAbsoluteBearing));
        setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));
        setFire(bulletPower);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        // Desviar das balas inimigas
        setTurnRightRadians(Utils.normalRelativeAngle(e.getBearingRadians() + Math.PI / 2));
        setAhead(100);
    }

    public void onHitWall(HitWallEvent e) {
        // Evitar colisões com paredes
        setBack(100);
        setTurnRightRadians(Math.PI / 2);
    }

    private class EnemyInfo {
        private double lastAbsoluteBearing;
        private double lastDistance;
        private double lastVelocity;
        private double lastHeading;

        public void update(double absoluteBearing, double distance, double velocity, double heading) {
            lastAbsoluteBearing = absoluteBearing;
            lastDistance = distance;
            lastVelocity = velocity;
            lastHeading = heading;
        }

        public double predictX(long time) {
            return getX() + Math.sin(lastAbsoluteBearing) * lastDistance + Math.sin(lastHeading) * lastVelocity * time;
        }

        public double predictY(long time) {
            return getY() + Math.cos(lastAbsoluteBearing) * lastDistance + Math.cos(lastHeading) * lastVelocity * time;
        }

        public double getHeading() {
            return lastHeading;
        }

        public double getVelocity() {
            return lastVelocity;
        }
    }
}