package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.Car;
import za.co.entelect.challenge.entities.GameState;
import za.co.entelect.challenge.entities.Lane;
import za.co.entelect.challenge.enums.PowerUps;
import za.co.entelect.challenge.enums.Terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.max;

public class Bot {

    private static final int maxSpeed = 9;
    private List<Integer> directionList = new ArrayList<>();

    private Random random;
    private GameState gameState;
    private Car opponent;
    private Car myCar;
    private final static Command FIX = new FixCommand();
    private final static Command ACCELERATE = new AccelerateCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command OIL = new OilCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command EMP = new EmpCommand();

    private final static Command TURN_RIGHT = new ChangeLaneCommand(1);
    private final static Command TURN_LEFT = new ChangeLaneCommand(-1);

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;

        directionList.add(-1);
        directionList.add(1);
    }

    public Command run() {
        // myCar Location
        int carLane = myCar.position.lane;
        int carBlock = myCar.position.block;

        // opponent Location
        int oppLane = opponent.position.lane;
        int oppBlock = opponent.position.block;

        List<Object> blocks = getBlocksInFront(carLane, myCar.position.block);
        List<Object> nextBlocks = blocks.subList(0,1);
        List<Object> blocksRight = getBlocksInFront(carLane, myCar.position.block);
        List<Object> blocksLeft = getBlocksInFront(carLane, myCar.position.block);
        if (carLane != 4) {
            blocksRight = getBlocksInFront(carLane+1, carBlock - 1);
        }
        if (carLane != 1) {
            blocksLeft = getBlocksInFront(carLane-1, carBlock-1);
        }
        List<Object> nextBlocksRight = blocksRight.subList(0,1);
        List<Object> nextBlocksLeft = blocksLeft.subList(0,1);

        if (myCar.damage >= 4){
            return FIX;
        }

        if (isClear(blocks) && myCar.damage == 0 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.speed < 15) {
            return BOOST;
        }
        if (isClear(blocksRight) && carLane != 4){
            return TURN_RIGHT;
        }
        if (isClear(blocksLeft) && carLane != 1){
            return TURN_LEFT;
        }

        if (myCar.speed <= 3){
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage < 3){
                return BOOST;
            } else {
                return ACCELERATE;
            }
        }

        if (hasPowerUp(PowerUps.EMP, myCar.powerups) && carBlock < oppBlock && (carLane - carBlock) * (carLane - carBlock) < 4 && opponent.damage < 4){
            return EMP;
        } else if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || nextBlocks.contains(Terrain.WALL))){
            return LIZARD;
        }

        if (!(containsPowerUp1(blocks)) && !isClear(blocks)){
            if (carBlock > oppBlock) {
                if (carLane != 4 && ((containsPowerUp1(blocksRight)) || containsPowerUp1(blocksRight))){
                    return TURN_RIGHT;
                } else if (carLane != 1 && ((containsPowerUp1(blocksLeft)) || containsPowerUp1(blocksLeft))){
                    return TURN_LEFT;
                }
            } else {
                if (carLane != 4 && ((containsPowerUp2(blocksRight)) || containsPowerUp2(blocksRight))){
                    return TURN_RIGHT;
                } else if (carLane != 1 && ((containsPowerUp2(blocksLeft)) || containsPowerUp2(blocksLeft))){
                    return TURN_LEFT;
                }
            }
        }

        /*
        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || nextBlocks.contains(Terrain.WALL)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            } else {
                if (carLane == 1){
                    return TURN_RIGHT;
                } else if (carLane == 4) {
                    return TURN_LEFT;
                }
            }
        }

        if (blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || nextBlocks.contains(Terrain.WALL)) {
            if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)) {
                return LIZARD;
            } else {
                if (nextBlocks.contains(Terrain.WALL)){
                    if (carLane == 4){
                        return TURN_LEFT;
                    } else if (carLane == 1){
                        return TURN_RIGHT;
                    } else if (nextBlocksRight.contains(Terrain.WALL)){
                        return TURN_LEFT;
                    } else if (nextBlocksLeft.contains(Terrain.WALL)){
                        return TURN_RIGHT;
                    } else if (availableDistance(blocksLeft) > availableDistance(blocksRight)){
                        return TURN_LEFT;
                    } else if (availableDistance(blocksLeft) == availableDistance(blocksRight)){
                        if (carLane == 2){
                            return TURN_RIGHT;
                        } else {
                            return TURN_LEFT;
                        }
                    } else {
                        return TURN_RIGHT;
                    }
                }
            }
        }

        if (availableDistance(blocksRight) > availableDistance(blocks) && availableDistance(blocksRight) > availableDistance(blocksLeft)){
            return TURN_RIGHT;
        } else if (availableDistance(blocksLeft) > availableDistance(blocks)){
            return TURN_LEFT;
        }
        */

        if (myCar.damage >= 3){
            return new FixCommand();
        }

        if (hasPowerUp(PowerUps.TWEET, myCar.powerups) && opponent.damage < 4){
            return new TweetCommand(oppLane, oppBlock+1);
        }
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && carBlock > oppBlock){
            return OIL;
        }
        /*
        if (availableDistance(blocks) >= 15 && hasPowerUp(PowerUps.BOOST, myCar.powerups)){
            return BOOST;
        }
        */
        if (myCar.damage != 0 && myCar.speed <= 8){
            return new FixCommand();
        }

        return ACCELERATE;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksInFront(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + Bot.maxSpeed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private int availableDistance(List<Object> blocks){
        int count = 0;
        for (int i = 0; i < 20; i++){
            if (blocks.subList(i, i).contains(Terrain.EMPTY) || blocks.subList(i, i).contains(Terrain.BOOST) || blocks.subList(i, i).contains(Terrain.OIL_POWER) || blocks.subList(i, i).contains(Terrain.LIZARD) || blocks.subList(i, i).contains(Terrain.EMP) || blocks.subList(i, i).contains(Terrain.TWEET)){
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private boolean isClear(List<Object> blocks){
        return !(blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL));
    }

    private boolean containsPowerUp1(List<Object> blocks){
        return (blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER));
    }
    private boolean containsPowerUp2(List<Object> blocks){
        return (blocks.contains(Terrain.EMP) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.TWEET) || blocks.contains(Terrain.OIL_POWER));
    }

    private Boolean hasPowerUp(PowerUps powerUpToCheck, PowerUps[] available) {
        for (PowerUps powerUp: available) {
            if (powerUp.equals(powerUpToCheck)) {
                return true;
            }
        }
        return false;
    }


}

