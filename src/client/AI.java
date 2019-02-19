package client;

import client.model.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AI
{

    private Logger logger;


    private Random random = new Random();

    public void preProcess(World world)
    {
        PropertyConfigurator.configure("log4j.properties");
        logger = Logger.getLogger("SOSPathPlanning");
//        System.out.println("pre process started");
    }

    public void pickTurn(World world)
    {
//        System.out.println("pick started");
//        for (HeroName v : HeroName.values())
//            System.out.println(v.toString());
        world.pickHero(HeroName.values()[world.getCurrentTurn()]);
    }

    public void moveTurn(World world)
    {
        long t1 = System.currentTimeMillis();
//        System.out.println("move started");
        Hero[] heroes = world.getMyHeroes();
        Cell[] objectiveZones = world.getMap().getObjectiveZone();
        List<Cell> blockedCells = new ArrayList<>();

//        if (objectiveZones[0] == null)
//            System.out.println("nulllllllllllll");

        int i = 0;
        for (Hero hero : heroes)
        {
//            if (objectiveZones[0] == null)
//                System.out.println("nulllllllllllll22222222");
//            if (hero.getCurrentCell() == null)
//                System.out.println("nulllllllllllll333333333333");

            Direction[] dirs = world.getPathMoveDirections(hero.getCurrentCell(), objectiveZones[i], blockedCells);
//            System.out.println("size: " + dirs.length);
            if (dirs.length != 0) {
                world.moveHero(hero, dirs[0]);
                Cell c;
                int row = hero.getCurrentCell().getRow();
                int col = hero.getCurrentCell().getColumn();
                if (dirs[0] == Direction.UP)
                    c = world.getMap().getCell(row + 1, col);
                else if (dirs[0] == Direction.RIGHT)
                    c = world.getMap().getCell(row, col + 1);
                else if (dirs[0] == Direction.DOWN)
                    c = world.getMap().getCell(row - 1, col);
                else
                    c = world.getMap().getCell(row, col - 1);
                blockedCells.add(c);
            }else{
//                System.out.println("empty " + i);
                blockedCells.add(hero.getCurrentCell());
            }

            i++;
        }
        long t2 = System.currentTimeMillis();
        System.out.println("elapsed time: " + (t2 - t1));
    }

    public void actionTurn(World world) {
        System.out.println("action started");
        Hero[] heroes = world.getMyHeroes();
        Map map = world.getMap();
        for (Hero hero : heroes)
        {
            int row = random.nextInt(map.getRowNum());
            int column = random.nextInt(map.getColumnNum());

            world.castAbility(hero, hero.getAbilities()[random.nextInt(3)], row, column);
        }
    }

    public double expectedDamage(World world){
        world.getOppCastAbilities()[0].getAbilityName();
        return 0.0;
    }

    public double expectedScore(World world){

        return 0.0;
    }

}