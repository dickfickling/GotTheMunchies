/* Dick Fickling
 * 2011.10.20
 * This is a game called "Got The Munchies".
 * Cannons on the side of the screen fire computers into the sky,
 * the player controls a moving face/bucket with which to catch computers.
 * Points are accrued as the player catches computers, and
 * TODO:
    - as the player's score increases, the number of cannons per side increases.
    - the player loses when he has missed a predetermined number of computers.
     
    - Refactoring: lots of code repetition
    - Access modifiers/visibility
    - 
*/


import java.util.*;
import world.*;
import image.*;
//import tester.*;

//> Modifiables
final class Constants{
    static final int HEIGHT = 800;
    static final int WIDTH = 500;
    static final int CANNONSIZE = 100;
    static final int FACESIZE = 100;
    static final int LEEWAY = 40;
    static final int COMPSPEED = 30;
    static final int TICKSPEED = 20;
    static final int CANNONSPEED = 100;
    static final double FRICTION = .7;
    static final int GRAVITY = 1;
    static final int TOPANGLE = 80;
    static final int BOTANGLE = 20;
    static final int TEXTSIZE = 30;
    static final String TEXTCOLOR = "black";
    static final Image SUN = new FromFile("img/sun.png");
    static final Image CANNONLEFT = new FromFile("img/cannonLeft.png");
    static final Image CANNONRIGHT = new FromFile("img/cannonRight.png");
    static final Image COMPUTER1 = new FromFile("img/computer1.png");
    static final Image COMPUTER2 = new FromFile("img/computer2.png");
    static final Image FACE = new FromFile("img/face.png");
    static final Image SKY = new Rectangle(WIDTH, HEIGHT, "solid", "sky blue");
    static final Image GRASS = new Rectangle(WIDTH, HEIGHT/6, "solid",
                                            "mediumseagreen");
    static final Scene BACKGROUND = 
        new EmptyScene(WIDTH, HEIGHT)
                                     .placeImage(SKY, WIDTH/2, HEIGHT/2)
                                     .placeImage(SUN, WIDTH*.95, 0)
                                     .placeImage(GRASS, WIDTH/2, HEIGHT*11/12);
}

//> A Posn is a location representation, with x and y
class Posn{
    double x;
    double y;
    
    Posn(double x, double y){
        this.x = x;
        this.y = y;
    }
    
    //> Is this Posn within 25px of the given one?
    boolean isCloseTo(Posn given){
        return (Math.abs(given.x - this.x) < Constants.LEEWAY) &&
               (Math.abs(given.y - this.y) < Constants.LEEWAY);
    }
    
    //> Convert the given angle (in degrees) to a Posn
    Posn angleToPosn(double angle){
        return new Posn(Math.cos(angle*Math.PI/180), 
                        Math.sin(angle*Math.PI/180));
    }
}

//> A Computer has a location, but also moves
class Computer extends Posn{
    double vx;
    double vy;
    double ang;
    double vr;
    int value;
    int picNum;
    
    Computer(double x,
            double y,
            double vx,
            double vy,
            double ang,
            double vr,
            int value,
            int picNum){
        super(x, y);
        this.vx = vx;
        this.vy = vy;
        this.ang = ang;
        this.vr = vr;
        this.value = value;
        this.picNum = picNum;
        
    }
    
    Computer(double x, double y){
        this(x, y,
            Constants.COMPSPEED*(Math.random()-0.5),
            -Constants.COMPSPEED*Math.random()-10,
            0,
            20*(Math.random()-0.5),
            10,
            new Random().nextInt(2));
    }
    
    Computer(Cannon c){
        this(c.getSide(),
             c.getHeight(),
             Constants.COMPSPEED*c.angleToPosn(c.getAngle()).x,
             -Constants.COMPSPEED*c.angleToPosn(c.getAngle()).y,
             0,
             20*(Math.random()-0.5),
             10,
             new Random().nextInt(2));
        if(c.getSide() > Constants.HEIGHT/2){
            this.vx = 0-this.vx;
        }
     }
    
    //> Draw this Computer on the given scene
     Scene drawOn(Scene given){
        if(picNum == 0){
            return given.placeImage(
            Constants.COMPUTER1.rotate(ang), this.x, this.y);
        }else{
            return given.placeImage(
            Constants.COMPUTER2.rotate(ang), this.x, this.y);
        }
     }
     
     //> Move this Computer one step
     void step(){
        if(this.x > Constants.WIDTH || this.x < 0){
            this.vx = 0-this.vx;
        }
        this.x = this.x+this.vx;
        this.y = this.y+this.vy;
        this.ang = (this.ang+this.vr) % 360;
        this.vy += Constants.GRAVITY;
        if(this.y >= Constants.HEIGHT){
            this.vy = 0-(this.vy*Constants.FRICTION);
            this.y = Constants.HEIGHT;
            if(this.value == 5) { this.value = 2; }
            if(this.value == 10) { this.value = 5; }
        }
    }
    
    //> Move this trash computer one step
    void stepTrash(){
        this.x = this.x+this.vx;
        this.y = this.y+this.vy;
        this.ang = (this.ang+this.vr) % 360;
        this.vy += Constants.GRAVITY;
        if(this.y >= Constants.HEIGHT){
            this.vy = 0-(this.vy*Constants.FRICTION);
            this.y = Constants.HEIGHT;
        }
    }
    
    int getValue(){
        return this.value;
    }
    
    //> Is this computer currently falling?
    boolean isDropping(){
        return this.vy > 0;
    }
    
    //> Can this computer bounce high enough to be eaten?
    boolean canBounceHigh(){
        return this.y < Constants.HEIGHT*.75 ||
                (this.y - ((this.vy*(this.vy+1)) / 2)) < Constants.HEIGHT*.95;
    }
    
    //> Is this Computer off the screen?
    boolean offScreen(){
        return this.x < 0 || this.x > Constants.WIDTH;
    }
}

//> A Face has a location, but only the x-variable matters
class Face extends Posn{

    Face(double x, double y){
        super(x, y);
     }
     
     Face(double x){
        super(x, Constants.HEIGHT*.75);
     }
     
     //> Draw this face on the given scene
     Scene drawOn(Scene given){
        return given.placeImage(
            Constants.FACE, this.x, this.y-Constants.FACESIZE/4);
     }
     
     //> Move this Face to the given x-coordinate
     void moveTo(int x){
        this.x = x;
     }
     
     //> Is the Face eating the given Computer?
     boolean isEating(Computer given){
        return this.isCloseTo(given) && 
               given.isDropping();
     }
}

//> A Cannon sits on the side, firing Computers
class Cannon extends Posn{
    double ang;
    String onside;
    Image cannonPic;
    int atTick;
    double idealAng;
    
    Cannon(double ang, String onside, double x, double y){
        super(x, y);
        this.ang = ang;
        this.onside = onside;
        if(onside.equals("left")){
            this.cannonPic = Constants.CANNONLEFT;
        }else{
            cannonPic = Constants.CANNONRIGHT;
        }
        newAngle();
        resetAtTick();
    }
    
    
    
    Cannon(String onside){
        super(Constants.CANNONSIZE*.5, Constants.HEIGHT*.5);
        this.ang = 0;
        this.onside = onside;
        resetAtTick();
        if(onside.equals("left")){
            this.cannonPic = Constants.CANNONLEFT;
        }else{
            cannonPic = Constants.CANNONRIGHT;
            this.x = Constants.WIDTH-Constants.CANNONSIZE*.5;
            this.atTick = Constants.CANNONSPEED/2;
        }
        newAngle();
    }
    
    double getSide(){
        return this.x;
    }
    
    double getHeight(){
        return this.y;
    }
    
    double getAngle(){
        return this.ang;
    }
    
    //> Reset this Cannon's atTick to 0
    void resetAtTick(){
        this.atTick = 0;
    }
    
    //> Increase this Cannon's atTick
    void increaseAtTick(){
        this.atTick = (this.atTick+1 % Constants.CANNONSPEED);
    }
    
    //> Draw this Cannon on the given scene
    Scene drawOn(Scene given){
        if(onside.equals("left")){
            return given.placeImage(
                            cannonPic.rotate(this.ang), this.x, this.y);
        }else{
            return given.placeImage(
                            cannonPic.rotate(0-this.ang), this.x, this.y);
        }
     }
    
    //> Rotate this Cannon
    void rotateCloser(){
        if(this.ang < (int) this.idealAng){
            this.ang+=1;
        }else if(this.ang > (int) this.idealAng){
            this.ang-=1;
        }
    }
    
    //> Generate a new angle for this cannon
    void newAngle(){
        this.idealAng = (Constants.TOPANGLE-Constants.BOTANGLE)*Math.random()
                        +Constants.BOTANGLE;
    }
    
    //> Ready to Fire?
    boolean readyToFire(){
        return atTick == Constants.CANNONSPEED;
    }
    
}

//> MunchiesWorld represents a GotTheMunchies game
class MunchiesWorld extends VoidWorld{
    ArrayList<Computer> trash;
    ArrayList<Computer> flying;
    ArrayList<Cannon> left;
    ArrayList<Cannon> right;
    Face face;
    int score;
    
    MunchiesWorld(ArrayList<Computer> trash,
                    ArrayList<Computer> flying,
                    ArrayList<Cannon> left,
                    ArrayList<Cannon> right,
                    Face face,
                    int score){
        this.trash = trash;
        this.flying = flying;
        this.left = left;
        this.right = right;
        this.face = face;
        this.score = score;
    }
    
    MunchiesWorld(){
        this(new ArrayList<Computer>(),
                new ArrayList<Computer>(),
                new ArrayList<Cannon>(),
                new ArrayList<Cannon>(),
                new Face(100),
                0);
        left.add(new Cannon("left"));
        right.add(new Cannon("right"));
    }
    
    //> Draw Cannons on given Scene
    Scene drawCannons(Scene given){
        for(Cannon c : left){
            given = c.drawOn(given);
        }
        for(Cannon c : right){
            given = c.drawOn(given);
        }
        return given;
    }
    
    //> Draw Computers on given Scene
    Scene drawComputers(Scene given){
        for(Computer c : trash){
            given = c.drawOn(given);
        }
        for(Computer c : flying){
            given = c.drawOn(given);
        }
        return given;
    }
    
    //> Draw the current score on the Scene given
    Scene drawScore(Scene given){
        String scoreString = "Score " + this.score;
        return given.placeImage(new Text(scoreString,
                                         Constants.TEXTSIZE,
                                         Constants.TEXTCOLOR),
                                Constants.WIDTH*.8, 
                                Constants.HEIGHT*.1);
    } 
    
    //> Draw everything on the Scene
    public Scene onDraw(){
        return drawCannons(
                drawComputers(
                    face.drawOn(
                        drawScore(
                            Constants.BACKGROUND))));
    }
    
    //> What to do with mouse input?
    public void onMouse(int x, int y, String me){
        face.moveTo(x);
    }
    
    //> Eat all computers near the Face
    void eatComputers(){
        for(int i=0; i<flying.size(); i++){
            Computer c = flying.get(i);
            if(face.isEating(c)){
                score+=c.getValue();
                flying.remove(i);
            }
        }
    }
    
    //> Move or fire all Cannons
    void doCannons(){
        for(Cannon c : left){
            if(c.readyToFire()){
                flying.add(new Computer(c));
                c.resetAtTick();
                c.newAngle();
            }else{
                c.increaseAtTick();
                c.rotateCloser();
            }
        }
        for(Cannon c : right){
            if(c.readyToFire()){
                flying.add(new Computer(c));
                c.resetAtTick();
                c.newAngle();
            }else{
                c.increaseAtTick();
                c.rotateCloser();
            }
        }
    }  
                
    
    //> Move all computers
    void stepAllComputers(){
        for(int i=0; i<trash.size(); i++){
            Computer c = trash.get(i);
            if(c.offScreen()){
                trash.remove(i);
            }else{
                c.stepTrash();
            }
        }
        for(int i=0; i<flying.size(); i++){
            Computer c = flying.get(i);
            if(c.canBounceHigh()){
                c.step();
            }else{
                flying.remove(i);
                trash.add(c);
            }
        }
    }
    
    //> Do this every tick
    public void onTick(){
        eatComputers();
        stepAllComputers();
        doCannons();
    }
}

class Examples{

    MunchiesWorld mu1 = new MunchiesWorld();
    VoidWorld v1 = mu1.bigBang();
    
    public static void main(String[] s)
    { new MunchiesWorld().bigBang(); }
    /*
    Computer co1;
    Computer co2;
    Computer co3;
    Computer co4;
    Computer co5;
    Cannon ca1;
    Cannon ca2;
    Face fa1;
    Face fa2;
    ArrayList<Computer> lco1;
    ArrayList<Computer> lco2;
    ArrayList<Cannon> lca1;
    ArrayList<Cannon> lca2;
    MunchiesWorld mu1;
    MunchiesWorld mu2;
    VoidWorld v1;
    
    
    void reset(){
    
    co1 = new Computer(20, 20, 5, 5, 30, 7, 10, 1);
    co2 = new Computer(150, 80, -3, 4, 20, 6, 10, 1);
    co3 = new Computer(30, 290, 4, -8, 0, 12, 10, 1);
    co4 = new Computer(180, 180, -4, -8, 0, 12, 10, 0);
    co5 = new Computer(40, 40, -50, 5, 30, 7, 10, 0);
    ca1 = new Cannon("left");
    ca2 = new Cannon("right");
    fa1 = new Face(200);
    fa2 = new Face(100);
    lco1 = new ArrayList<Computer>();
    lco1.add(co1);
    lco1.add(co2);
    lco2 = new ArrayList<Computer>();
    lca1 = new ArrayList<Cannon>();
    lca1.add(ca1);
    lca2 = new ArrayList<Cannon>();
    lca2.add(ca2);
    mu1 = new MunchiesWorld(lco2, lco1, lca1, lca2, fa1, 0);
    mu2 = new MunchiesWorld();
    
    //v1 = mu2.bigBang();
    }
    
    MunchiesWorldExamples(){ reset(); }
    
    boolean testIsCloseTo(Tester t){
        reset();
        return (t.checkExpect(co1.isCloseTo(co5), true) &&
               t.checkExpect(co1.isCloseTo(co4), false));
    }
    
    boolean testAngleToPosn(Tester t){
        reset();
        Posn po1 = new Posn(1, 0);
        Posn po2 = new Posn(Math.sqrt(3)/2, .5);
        return (t.checkExpect(co1.angleToPosn(0), po1) &&
                t.checkInexact(co1.angleToPosn(30), po2, .001));
    }
    
    boolean testStepComputer(Tester t){
        reset();
        boolean acc = true;
        acc = acc && t.checkExpect(co1.x, 20.0) && t.checkExpect(co1.y, 20.0);
        co1.step();
        acc = acc && t.checkExpect(co1.x, 25.0) && 
                     t.checkExpect(co1.y, 25.0) &&
                     t.checkExpect(co1.vy, 6.0);
        return acc;
    }
    
    boolean testIsDropping(Tester t){
        reset();
        boolean acc = true;
        acc = acc && t.checkExpect(co1.isDropping(), true) && 
                     t.checkExpect(co3.isDropping(), false);
        return acc;
    }
    
    */
    

    
}


