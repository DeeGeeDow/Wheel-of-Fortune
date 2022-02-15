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

        List<Object> blocks = getBlocksMax(carLane, myCar.position.block);
        List<Object> nextBlocks = getBlocksReach(carLane, myCar.position.block);

        List<Object> blocksRight = getBlocksMax(carLane, myCar.position.block);
        List<Object> blocksLeft = getBlocksMax(carLane, myCar.position.block);
        List<Object> nextBlocksRight = getBlocksReach(carLane, carBlock);
        List<Object> nextBlocksLeft = getBlocksReach(carLane, carBlock);

        if (carLane != 4) {
            blocksRight = getBlocksMax(carLane+1, carBlock - 1);
            nextBlocksRight = getBlocksReach(carLane+1, carBlock - 1);
        }
        if (carLane != 1) {
            blocksLeft = getBlocksMax(carLane-1, carBlock-1);
            nextBlocksLeft = getBlocksReach(carLane-1, carBlock-1);
        }

        if (myCar.damage >= 4){
            return FIX;
        }

        if (myCar.speed <= 3){
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage < 3 && isClear(blocks)){
                return BOOST;
            } else {
                return ACCELERATE;
            }
        }

        if (isClear(blocks) && myCar.damage == 0 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.boostCounter <= 1) {
            return BOOST;
        }
        if (myCar.boosting && !isClear(blocks) && isClear(blocksRight) && carLane != 4){
            return TURN_RIGHT;
        }
        if (myCar.boosting && !isClear(blocks) && isClear(blocksLeft) && carLane != 1){
            return TURN_LEFT;
        }
        if (myCar.boosting && !isClear(blocks) && hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
            return LIZARD;
        }
        if (!isClear(nextBlocks) && isClear(blocksRight) && carLane != 4){
            return TURN_RIGHT;
        }
        if (!isClear(nextBlocks) && isClear(blocksLeft) && carLane != 1){
            return TURN_LEFT;
        }
        if (isClear(blocksRight) && carLane != 4){
            if (carBlock > oppBlock && containsPowerUp1(nextBlocksRight) && !containsPowerUp1(nextBlocks)) {
                return TURN_RIGHT;
            } else if (carBlock <= oppBlock && containsPowerUp2(nextBlocksRight) && !containsPowerUp2(nextBlocks)){
                return TURN_RIGHT;
            }
        }
        if (isClear(blocksLeft) && carLane != 1){
            if (carBlock > oppBlock && containsPowerUp1(nextBlocksLeft) && !containsPowerUp1(nextBlocks)) {
                return TURN_LEFT;
            } else if (carBlock <= oppBlock && containsPowerUp2(nextBlocksLeft) && !containsPowerUp2(nextBlocks)){
                return TURN_LEFT;
            }
        }

        if (hasPowerUp(PowerUps.EMP, myCar.powerups) && carBlock < oppBlock && (carLane - carBlock) * (carLane - carBlock) < 4 && opponent.damage < 4){
            return EMP;
        }

        if (!(containsPowerUp1(nextBlocks)) && !isClear(nextBlocks) && !myCar.boosting){
            if (carBlock > oppBlock) {
                if (carLane != 4 && ((containsPowerUp1(nextBlocksRight)) || containsPowerUp1(nextBlocksRight))){
                    return TURN_RIGHT;
                } else if (carLane != 1 && ((containsPowerUp1(nextBlocksLeft)) || containsPowerUp1(nextBlocksLeft))){
                    return TURN_LEFT;
                }
            } else {
                if (carLane != 4 && ((containsPowerUp2(nextBlocksRight)) || containsPowerUp2(nextBlocksRight))){
                    return TURN_RIGHT;
                } else if (carLane != 1 && ((containsPowerUp2(nextBlocksLeft)) || containsPowerUp2(nextBlocksLeft))){
                    return TURN_LEFT;
                }
            }
        }
        if (!(containsPowerUp1(blocks)) && !isClear(blocks) && myCar.boosting){
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

        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups) && !isClear(nextBlocks)){
            return LIZARD;
        }

        if (myCar.damage >= 3){
            return new FixCommand();
        }

        if (myCar.damage != 0 && myCar.speed <= 8){
            return new FixCommand();
        }

        if (myCar.boostCounter <= 1 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && isClear(blocks)){
            return BOOST;
        }

        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && carBlock > oppBlock){
            return OIL;
        }

        if (myCar.speed >= 9 && hasPowerUp(PowerUps.TWEET, myCar.powerups) && carBlock > oppBlock){
            if (opponent.speed > 9) {
                return new TweetCommand(oppLane, oppBlock+15);
            } else if (opponent.speed == 9 && !hasPowerUp(PowerUps.BOOST, opponent.powerups)){
                return new TweetCommand(oppLane, oppBlock+9);
            }
        }

        return ACCELERATE;
    }

    /**
     * Returns map of blocks and the objects in the for the current lanes, returns the amount of blocks that can be
     * traversed at max speed.
     **/
    private List<Object> getBlocksMax(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + 15; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private List<Object> getBlocksReach(int lane, int block) {
        List<Lane[]> map = gameState.lanes;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;

        Lane[] laneList = map.get(lane - 1);
        for (int i = max(block - startBlock, 0); i <= block - startBlock + maxSpeed; i++) {
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }

            blocks.add(laneList[i].terrain);

        }
        return blocks;
    }

    private int dangerOnLane(List<Object> blocks){
        int count = 0;
        for (int i = 0; i < 9; i++){
            if (blocks.subList(i, i).contains(Terrain.OIL_SPILL) || blocks.subList(i, i).contains(Terrain.MUD)){
                count++;
            } else if (blocks.subList(i, i).contains(Terrain.WALL)){
                count += 2;
            }
        }
        return count;
    }

    private boolean isClear(List<Object> blocks){
        return !(blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL));
    }

    private boolean containsPowerUp1(List<Object> blocks){
        return (blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.BOOST) || blocks.contains(Terrain.OIL_POWER));
    }
    private boolean containsPowerUp2(List<Object> blocks){
        return (blocks.contains(Terrain.EMP) || blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.BOOST));
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

