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
        // myCar Position
        int carLane = myCar.position.lane;
        int carBlock = myCar.position.block;

        // opponent Position
        int oppLane = opponent.position.lane;
        int oppBlock = opponent.position.block;

        // Get blocks information front and sides (if exist)
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

        // If car too damaged
        // -> FIX
        if (myCar.damage >= 4){
            return FIX;
        }

        // If moving too slow
        // -> Use BOOST or ACCELERATE
        if (myCar.speed <= 3){
            if (hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.damage < 3 && isClear(blocks)){
                return BOOST;
            } else {
                return ACCELERATE;
            }
        }

        // Lane is clear ahead, car is undamaged, boost is running out / inactive, have BOOST
        // -> Use BOOST
        if (isClear(blocks) && myCar.damage == 0 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && myCar.boostCounter <= 1) {
            return BOOST;
        }

        // If car is boosting, current lane has obstacle
        // -> Turn to empty Lane, prioritizing going to Lane 2 or 3
        if (myCar.boostCounter >= 1 && !isClear(blocks)){
            if (isClear(blocksRight) && carLane != 4){
                if (carLane == 3 && isClear(blocksLeft)){
                    return TURN_LEFT;
                } else {
                    return TURN_RIGHT;
                }
            } else if (isClear(blocksLeft) && carLane != 1){
                return TURN_LEFT;
            }
        }

        // If car is boosting, current lane has obstacle, have LIZARD
        // -> Use LIZARD
        if (myCar.boostCounter > 1 && hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
            if (!nextBlocks.contains(Terrain.FINISH)){
                if (!isClear(blocks.subList(0, blocks.size()-3))){
                    return LIZARD;
                }
            } else if (!isClear(nextBlocks)){
                return LIZARD;
            }
        }

        // (If car is not boosting), current lane has obstacle
        // -> Turn to empty Lane, prioritizing Lane 2 or 3
        if (!isClear(nextBlocks) && isClear(blocksRight) && carLane != 4){
            if (isClear(blocksLeft) && carLane == 3){
                return TURN_LEFT;
            } else {
                return TURN_RIGHT;
            }
        } else if (!isClear(nextBlocks) && isClear(blocksLeft) && carLane != 1){
            return TURN_LEFT;
        }

        // If current lane is clear but doesn't have any PowerUp to pick up
        // -> Go to empty Lane with PowerUp, prioritizing Lane 2 or 3
        if (isClear(blocksRight) && carLane != 4){
            if (isClear(blocksLeft) && carLane == 3){
                if (carBlock > oppBlock && containsPowerUp1(nextBlocksLeft) && !containsPowerUp1(nextBlocks)) {
                    return TURN_LEFT;
                } else if (carBlock <= oppBlock && containsPowerUp2(nextBlocksLeft) && !containsPowerUp2(nextBlocks)){
                    return TURN_LEFT;
                }
            } else {
                if (carBlock > oppBlock && containsPowerUp1(nextBlocksRight) && !containsPowerUp1(nextBlocks)) {
                    return TURN_RIGHT;
                } else if (carBlock <= oppBlock && containsPowerUp2(nextBlocksRight) && !containsPowerUp2(nextBlocks)){
                    return TURN_RIGHT;
                }
            }
        } else if (isClear(blocksLeft) && carLane != 1){
            if (carBlock > oppBlock && containsPowerUp1(nextBlocksLeft) && !containsPowerUp1(nextBlocks)) {
                return TURN_LEFT;
            } else if (carBlock <= oppBlock && containsPowerUp2(nextBlocksLeft) && !containsPowerUp2(nextBlocks)){
                return TURN_LEFT;
            }
        }

        // If have EMP, behind enemy, current Lane is equal or adjacent to enemy Lane, enemy moving faster or equal to speed 6
        // -> Use EMP
        if (hasPowerUp(PowerUps.EMP, myCar.powerups) && carBlock < oppBlock && (carLane - carBlock) * (carLane - carBlock) < 4 && opponent.speed >= 6){
            return EMP;
        }

        // If current lane has obstacle and doesn't have any PowerUp to pick up
        // -> Turn to lane with PowerUp, prioritizing Lane 2 or 3
        if (!(containsPowerUp1(nextBlocks)) && !isClear(nextBlocks) && !myCar.boosting){
            if (carBlock > oppBlock) {
                if (carLane != 4 && containsPowerUp1(nextBlocksRight)){
                    if (carLane == 3 && containsPowerUp1(nextBlocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp1(nextBlocksLeft)){
                    return TURN_LEFT;
                }
            } else {
                if (carLane != 4 && containsPowerUp2(nextBlocksRight)){
                    if (carLane == 3 && containsPowerUp2(nextBlocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp2(nextBlocksLeft)){
                    return TURN_LEFT;
                }
            }
        } else if (!(containsPowerUp1(blocks)) && !isClear(blocks) && myCar.boosting){
            if (carBlock > oppBlock) {
                if (carLane != 4 && containsPowerUp1(blocksRight)){
                    if (carLane != 3 && containsPowerUp1(blocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp1(blocksLeft)){
                    return TURN_LEFT;
                }
            } else {
                if (carLane != 4 && containsPowerUp2(blocksRight)){
                    if (carLane == 3 && containsPowerUp2(blocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp2(blocksLeft)){
                    return TURN_LEFT;
                }
            }
        }

        // (If car is not boosting), have LIZARD, current Lane has obstacle
        // -> Use LIZARD
        if (hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
            if (!nextBlocks.contains(Terrain.FINISH)){
                if (!isClear(nextBlocks.subList(0, nextBlocks.size()-3))){
                    return LIZARD;
                }
            } else if (!isClear(nextBlocks)){
                return LIZARD;
            }
        }

        // Car is still damaged by more than 3 -> FIX
        if (myCar.damage >= 3){
            return FIX;
        }

        // Car is still damaged and is almost or going at max speed of 9
        // -> FIX
        if (myCar.damage != 0 && myCar.speed <= 8){
            return FIX;
        }

        // Car is not boosting or BOOST almost runs out, have BOOST, current Lane is clear
        // -> Use BOOST
        if (myCar.boostCounter <= 1 && hasPowerUp(PowerUps.BOOST, myCar.powerups) && isClear(blocks)){
            return BOOST;
        }

        // If car moving at max speed or more, have TWEET, ahead of enemy
        // -> Use TWEET depending on enemy speed
        if (myCar.speed >= 9 && hasPowerUp(PowerUps.TWEET, myCar.powerups) && carBlock > oppBlock){
            if (opponent.speed > 9) {
                return new TweetCommand(oppLane, oppBlock+16);
            } else if (opponent.speed == 9 && opponent.damage == 1){
                return new TweetCommand(oppLane, oppBlock+10);
            } else if (opponent.speed == 8 && opponent.damage == 2){
                return new TweetCommand(oppLane, oppBlock+9);
            }
        }

        // If car has OIL PowerUp and ahead of enemy
        // -> Use OIL PowerUp
        if (hasPowerUp(PowerUps.OIL, myCar.powerups) && carBlock > oppBlock){
            return OIL;
        }

        // If current lane doesn't have any PowerUp to pick up and moving at speed 8 or 9
        // -> Go to empty Lane with PowerUp, prioritizing Lane 2 or 3
        if (carBlock <= oppBlock){
            if ((myCar.speed == 8 || myCar.speed == 9) && !containsPowerUp2(nextBlocks)){
                if (carLane != 4 && containsPowerUp2(nextBlocksRight) && isClear(nextBlocksRight)) {
                    if (carLane == 3 && containsPowerUp2(nextBlocksLeft) && isClear(nextBlocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp2(nextBlocksLeft) && isClear(nextBlocksLeft)){
                    return TURN_LEFT;
                }
            }
        } else {
            if ((myCar.speed == 8 || myCar.speed == 9) && !containsPowerUp1(nextBlocks)){
                if (carLane != 4 && containsPowerUp1(nextBlocksRight) && isClear(nextBlocksRight)) {
                    if (carLane == 3 && containsPowerUp1(nextBlocksLeft) && isClear(nextBlocksLeft)){
                        return TURN_LEFT;
                    } else {
                        return TURN_RIGHT;
                    }
                } else if (carLane != 1 && containsPowerUp1(nextBlocksLeft) && isClear(nextBlocksLeft)){
                    return TURN_LEFT;
                }
            }
        }

        // If blocks in front of car is obstacle (including Cyber Truck) and has LIZARD
        // -> Use LIZARD
        if (
                !( blocks.subList(0, 1).contains(Terrain.EMP)
                || blocks.subList(0, 1).contains(Terrain.LIZARD)
                || blocks.subList(0, 1).contains(Terrain.EMPTY)
                || blocks.subList(0, 1).contains(Terrain.BOOST)
                || blocks.subList(0, 1).contains(Terrain.OIL_POWER)
                || blocks.subList(0, 1).contains(Terrain.TWEET)
                ) && hasPowerUp(PowerUps.LIZARD, myCar.powerups)){
            return LIZARD;
        }

        // None of the condition above is met
        // -> Accelerate
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

    private boolean isClear(List<Object> blocks){
        return !(blocks.contains(Terrain.MUD) || blocks.contains(Terrain.OIL_SPILL) || blocks.contains(Terrain.WALL));
    }

    private boolean containsPowerUp1(List<Object> blocks){
        return (blocks.contains(Terrain.LIZARD) || blocks.contains(Terrain.BOOST));
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

