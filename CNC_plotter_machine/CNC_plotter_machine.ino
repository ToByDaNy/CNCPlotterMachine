#include <Servo.h>
#include <Stepper.h>
#define LINE_BUFFER_LENGTH 512
//char STEP = MICROSTEP ;

const int penZUp = 80;
const int penZDown = 100;

const int penServoPin =11 ;

const int stepsPerRevolution = 5; 

Servo penServo;  

Stepper myStepperY(stepsPerRevolution,2,5);            
Stepper myStepperX(stepsPerRevolution,3,6);  


struct point { 
  float x; 
  float y; 
  float z; 
};


struct point actuatorPos;


float StepInc = 1;
int LineDelay = 0;
int penDelay = 50;

// Use test sketch to go 100 steps. Measure the length of line. 

float StepsPerMillimeterX = 1;
float StepsPerMillimeterY = 1;


float Xmin = 0;
float Xmax = 250;
float Ymin = 0;
float Ymax = 250;
float Zmin = 0;
float Zmax = 1;
float Xpos = Xmin;
float Ypos = Ymin;
float Zpos = Zmax; 

boolean verbose = true;




void setup() {
  
  Serial.begin( 9600 );
  
  penServo.attach(penServoPin);
  penServo.write(penZUp);
  delay(100);
  
  myStepperX.setSpeed(600);
  myStepperY.setSpeed(600);  
  
  Serial.print("X range is from "); 
  Serial.print(Xmin); 
  Serial.print(" to "); 
  Serial.print(Xmax); 
  Serial.println(" mm."); 
  Serial.print("Y range is from "); 
  Serial.print(Ymin); 
  Serial.print(" to "); 
  Serial.print(Ymax); 
  Serial.println(" mm."); 
}

void loop() 
{
  
  delay(100);
  char line[ LINE_BUFFER_LENGTH ];
  char c;
  int lineIndex;
  bool lineIsComment, lineSemiColon;
  lineIndex = 0;
  lineSemiColon = false;
  lineIsComment = false;
  while (1) {
    while (Serial.available()>0 ) {
      c = Serial.read();
      //Serial.println(c);
      if (( c == '\n') || (c == '\r') ) {             
        if ( lineIndex > 0 ) {                        
          line[ lineIndex ] = '\0';                  
          if (verbose) { 
            Serial.print( "Received : "); 
            Serial.println( line ); 
          }
          processIncomingLine( line, lineIndex );
          lineIndex = 0;
        } 
        lineIsComment = false;
        lineSemiColon = false;
        Serial.println("ok");    
      } 
      else {
        if ( (lineIsComment) || (lineSemiColon) ) {   
          if ( c == ')' )  lineIsComment = false;     
        } 
        else {
          if ( c <= ' ' ) {                          
          } 
          else if ( c == '/' ) {                   
          } 
          else if ( c == '(' ) {                 
            lineIsComment = true;
          } 
          else if ( c == ';' ) {
            lineSemiColon = true;
          } 
          else if ( lineIndex >= LINE_BUFFER_LENGTH-1 ) {
            Serial.println( "ERROR - lineBuffer overflow" );
            lineIsComment = false;
            lineSemiColon = false;
          } 
          else if ( c >= 'a' && c <= 'z' ) {        
            line[ lineIndex++ ] = c-'a'+'A';
          } 
          else {
            line[ lineIndex++ ] = c;
          }
        }
      }
    }
  }
}
void processIncomingLine( char* line, int charNB ) {
  int currentIndex = 0;
  char buffer[ 64 ];                                 
  struct point newPos;
  newPos.x = 0.0;
  newPos.y = 0.0;
  while( currentIndex < charNB ) {
    switch ( line[ currentIndex++ ] ) {            
    case 'U':
      penUp(); 
      break;
    case 'D':
      penDown(); 
      break;
    case 'G':

      buffer[0] = line[ currentIndex++ ];          
      //      buffer[1] = line[ currentIndex++ ];
      //      buffer[2] = '\0';
      buffer[1] = '\0';
      char* indexX = strchr( line+currentIndex, 'X' );  // Get X/Y position in the string (if any)
        char* indexY = strchr( line+currentIndex, 'Y' );
      switch ( atoi( buffer ) ){                 
      case 0:                              
             
        if ( indexY <= 0 ) {
          newPos.x = atof( indexX + 1); 
          newPos.y = actuatorPos.y;
        } 
        else if ( indexX <= 0 ) {
          newPos.y = atof( indexY + 1);
          newPos.x = actuatorPos.x;
        } 
        else {
          newPos.y = atof( indexY + 1);
          indexY = '\0';
          newPos.x = atof( indexX + 1);
        }
        drawLine2(newPos.x, newPos.y );

        actuatorPos.x = newPos.x;
        actuatorPos.y = newPos.y;
        break;
      case 1:
        if ( indexY <= 0 ) {
          newPos.x = atof( indexX + 1); 
          newPos.y = actuatorPos.y;
        } 
        else if ( indexX <= 0 ) {
          newPos.y = atof( indexY + 1);
          newPos.x = actuatorPos.x;
        } 
        else {
          newPos.y = atof( indexY + 1);
          indexY = '\0';
          newPos.x = atof( indexX + 1);
        }
        drawLine(newPos.x, newPos.y );

        actuatorPos.x = newPos.x;
        actuatorPos.y = newPos.y;
        break;
      }
      break;
    case 'M':
      buffer[0] = line[ currentIndex++ ];        
      buffer[1] = line[ currentIndex++ ];
      buffer[2] = line[ currentIndex++ ];
      buffer[3] = '\0';
      switch ( atoi( buffer ) ){
      case 300:
        {
          char* indexS = strchr( line+currentIndex, 'S' );
          float Spos = atof( indexS + 1);

          if (Spos == 100) { 
            penDown(); 
          }
          if (Spos == 80) { 
            penUp(); 
          }
          break;
        }
      case 000:                                // M000 - Repport position
        Serial.print( "Absolute position : X = " );
        Serial.print( actuatorPos.x );
        Serial.print( "  -  Y = " );
        Serial.println( actuatorPos.y );
        break;
      default:
        Serial.print( "Command not recognized : M");
        Serial.println( buffer );
      }
    }
  }
}

void drawLine(float x1, float y1) {
  if (verbose)
  {
    Serial.print("fx1, fy1: ");
    Serial.print(x1);
    Serial.print(",");
    Serial.print(y1);
    Serial.println("");
  }  
  
  if (x1 >= Xmax) { 
    x1 = Xmax; 
  }
  if (x1 <= Xmin) { 
    x1 = Xmin; 
  }
  if (y1 >= Ymax) { 
    y1 = Ymax; 
  }
  if (y1 <= Ymin) { 
    y1 = Ymin; 
  }
  if (verbose)
  {
    Serial.print("Xpos, Ypos: ");
    Serial.print(Xpos);
    Serial.print(",");
    Serial.print(Ypos);
    Serial.println("");
  }
  if (verbose)
  {
    Serial.print("x1, y1: ");
    Serial.print(x1);
    Serial.print(",");
    Serial.print(y1);
    Serial.println("");
  }
  //  Convert coordinates to steps
  x1 = (int)(x1*StepsPerMillimeterX);
  y1 = (int)(y1*StepsPerMillimeterY);
  float x0 = Xpos;
  float y0 = Ypos;
  //  Let's find out the change for the coordinates
  long dx = abs(x1-x0);
  long dy = abs(y1-y0);
  int sx = x0<x1 ? StepInc : -StepInc;
  int sy = y0<y1 ? StepInc : -StepInc;
  long i;
  long over = 0;
  if (dx > dy) {
    for (i=0; i<dx; ++i) {
      myStepperX.step(sx*stepsPerRevolution);
      over+=dy;
      if (over>=dx) {
        over-=dx;
        myStepperY.step(sy*stepsPerRevolution);
      }
    }
  }
  else {
    for (i=0; i<dy; ++i) {
      myStepperY.step(sy*stepsPerRevolution);
      over+=dx;
      if (over>=dy) {
        over-=dy;
        myStepperX.step(sx*stepsPerRevolution);
      }
    }    
  }
  if (verbose)
  {
    Serial.print("dx, dy:");
    Serial.print(dx);
    Serial.print(",");
    Serial.print(dy);
    Serial.println("");
  }
  if (verbose)
  {
    Serial.print("Going to (");
    Serial.print(x0);
    Serial.print(",");
    Serial.print(y0);
    Serial.println(")");
  }

  Xpos = x1;
  Ypos = y1;
}
void drawLine2(float x1, float y1) {
  if (verbose)
  {
    Serial.print("fx1, fy1: ");
    Serial.print(x1);
    Serial.print(",");
    Serial.print(y1);
    Serial.println("");
  }  
  
  if (x1 >= Xmax) { 
    x1 = Xmax; 
  }
  if (x1 <= Xmin) { 
    x1 = Xmin; 
  }
  if (y1 >= Ymax) { 
    y1 = Ymax; 
  }
  if (y1 <= Ymin) { 
    y1 = Ymin; 
  }
  if (verbose)
  {
    Serial.print("Xpos, Ypos: ");
    Serial.print(Xpos);
    Serial.print(",");
    Serial.print(Ypos);
    Serial.println("");
  }
  if (verbose)
  {
    Serial.print("x1, y1: ");
    Serial.print(x1);
    Serial.print(",");
    Serial.print(y1);
    Serial.println("");
  }
  //  Convert coordinates to steps
  x1 = (int)(x1*StepsPerMillimeterX);
  y1 = (int)(y1*StepsPerMillimeterY);
  float x0 = Xpos;
  float y0 = Ypos;
  //  Let's find out the change for the coordinates
  long dx = abs(x1-x0);
  long dy = abs(y1-y0);
  int sx = x0<x1 ? StepInc : ((-StepInc));
  int sy = y0<y1 ? StepInc : ((-StepInc));
  myStepperX.step(sx*dx*stepsPerRevolution);
  myStepperY.step(sy*dy*stepsPerRevolution);
     
  
  if (verbose)
  {
    Serial.print("dx, dy:");
    Serial.print(dx);
    Serial.print(",");
    Serial.print(dy);
    Serial.println("");
  }
  if (verbose)
  {
    Serial.print("Going to (");
    Serial.print(x0);
    Serial.print(",");
    Serial.print(y0);
    Serial.println(")");
  }
  Xpos = x1;
  Ypos = y1;
}
//  Raises pen
void penUp() { 
  penServo.write(penZUp); 
  delay(penDelay); 
  Zpos=Zmax; 
  digitalWrite(15, LOW);
  digitalWrite(16, HIGH);
  if (verbose) { 
    Serial.println("Pen up!"); 
  } 
}
//  Lowers pen
void penDown() { 
  penServo.write(penZDown); 
  delay(penDelay); 
  Zpos=Zmin; 
  digitalWrite(15, HIGH);
  digitalWrite(16, LOW);
  if (verbose) { 
    Serial.println("Pen down."); 
  } 
}