"""4.7 도안: 화산 거신 g · 폭풍의 기사왕 K · 구름 파수병 P · 천둥매 T · 구름 사제 A (Box 목록)."""
import sys,math,random
from cosmetic_design import *
S=solid
VOID=(6,6,14)
BG=(30,26,40)
OB=(20,14,20);OB2=(44,32,38);AS=(70,62,62);LV=(255,110,20);LV2=(255,190,60);HOT=(255,245,190);RU=(150,30,10)
def magma(seed,base=OB,alt=OB2,rate=11):
    return lambda f,u,v,w,h:(LV2 if (u*7+v*13+seed)%(rate*3)==0 else LV if (u*5+v*3+seed)%rate==0 or (u-2*v+seed)%(rate+6)==0 else (base if (u+v*2+seed)%4 else alt))
def plate(seed):
    return lambda f,u,v,w,h:(RU if v%5==4 and (u+seed)%3 else (OB if (u+seed)%5 else OB2))
g=[]
Y=-30
def G(x,y,z,w,h,d,p,):g.append(Box(x,y+Y,z,w,h,d,p if callable(p) else S(p)))
# 다리: 짧고 굵은 기둥, 발에서 용암이 흐름
for s,n in((-1,1),(1,2)):
    x=-22 if s<0 else 6
    G(x,0,-12,16,10,22,magma(n));G(x+1,10,-9,14,22,16,plate(n));G(x-1,18,-11,18,8,20,magma(n+3,OB2,AS))
    for i in range(4):G(x+1+i*4,0,10,3,3+(i%2)*2,2,LV2);G(x+1+i*4,0,12,2,1,3,LV)
    for i in range(3):G(x+2+i*5,6,-14,3,4,3,AS)
# 허리 · 몸통: 앞이 벌어진 흑요석 판, 안쪽 녹은 심장
G(-20,32,-12,40,14,22,plate(3));G(-26,46,-16,52,44,30,magma(4,OB,OB2,17))
G(-13,50,14,26,32,.5,S(RU));G(-10,53,14.5,20,26,1,S(LV));G(-7,57,15.5,14,18,1.2,S(LV2));G(-3.5,62,16.7,7,8,1,S(HOT))
for i in range(5):
    y=50+i*7
    for s in(-1,1):
        L=13-abs(i-2)*1.2
        G(s*14-(0 if s>0 else 0) if s>0 else -14-L,y,14,L,3,3,S(AS));G(-15 if s<0 else 13,y,12,2,3,5,S(OB2))
# 등 화산구 세 개와 분출
for x,z,h,w in((-20,-16,16,14),(6,-18,22,16),(-6,-22,12,12)):
    G(x,90,z,w,h,w,magma(x+40,AS,OB2,9));G(x+2,90+h,z+2,w-4,1,w-4,S(LV2));G(x+3,91+h,z+3,w-6,.6,w-6,S(HOT))
    for i in range(6):G(x+w/2-1+math.sin(i*1.7+x)*2.5,92+h+i*5,z+w/2-1+math.cos(i*1.3)*2.5,3-i*.25,4,3-i*.25,S(LV2 if i<2 else LV if i<4 else AS))
# 어깨 사이에 파묻힌 머리: 뿔 두개골, 턱에서 용암
G(-11,78,8,22,20,18,magma(9,OB2,AS,14));G(-12,92,8,24,5,19,S(AS))
G(-8,86,25.8,6,4,.5,S(HOT));G(2,86,25.8,6,4,.5,S(HOT));G(-9,85,25.6,8,6,.4,S(LV));G(1,85,25.6,8,6,.4,S(LV))
G(-8,73,10,16,7,17,S(OB2));G(-7,79,26.6,14,3,.6,S(LV2))
for i in range(-6,7,3):G(i,76,26.2,2,6-(abs(i)%3),.8,S((230,214,190)))
for i in range(5):G(-2+i*.8,73-i*5,24,3-i*.3,5,3-i*.3,S(LV2 if i<3 else LV))
for s in(-1,1):
    for i,(dx,dy,dz,w) in enumerate([(12,92,14,7),(19,98,14,6),(25,106,12,5),(28,116,9,4),(27,125,6,3)]):
        G(s*dx-w/2,dy,dz,w,10,w,S(AS if i<3 else (230,214,190)))
# 왼팔: 끝이 쇠사슬과 용암 사슬철퇴
for i in range(5):G(-44+i*3,78-i*9,-8,17-i*.6,12,17-i*.6,magma(20+i,OB,OB2,13) if i%2 else plate(i))
G(-52,82,-14,28,18,26,magma(31,AS,OB2,10))
for i in range(6):G(-36+(i%2)*1.5,26-i*4.2,-3,6,5,3 if i%2 else 7,S((90,86,92)))
G(-49,-8,-14,30,24,30,magma(33,OB,OB2,7))
for dx,dz in((-54,-1),(-22,-1),(-36,-20),(-36,17)):G(dx,0,dz,4,8,4,S(AS))
# 오른팔: 분화구 주먹
for i in range(4):G(30+i*2.5,80-i*12,-10,20,14,20,magma(40+i,OB,OB2,12))
G(28,86,-16,28,16,28,magma(45,AS,OB2,10))
G(26,26,-16,38,26,32,magma(47,OB2,AS,8));G(32,52,-9,26,4,18,S(LV2));G(35,56,-6,20,1,12,S(HOT))
for i in range(5):G(38+math.sin(i)*5,58+i*7,-2+math.cos(i*2)*4,5,6,5,S(LV2 if i<3 else LV))
for i in range(4):G(29+i*9,20,14,7,8,5,S((230,214,190)))
K=[];Q=-28
def Kx(x,y,z,w,h,d,p):K.append(Box(x,y+Q,z,w,h,d,p if callable(p) else S(p)))
SV=(176,186,208);SV2=(120,132,160);NV=(26,32,66);BL=(90,170,255);EL=(210,240,255);GO=(216,172,70);GO2=(146,104,40)
def arm(seed):return lambda f,u,v,w,h:(GO if u in(0,w-1) or v in(0,h-1) else (EL if (u*3+v+seed)%19==0 else SV if (u+v)%3 else SV2))
# 하반신 없음: 번개로 된 소용돌이와 구름
r=random.Random(3)
for i in range(11):
    a=i*.75;R=10+i*1.3
    Kx(math.cos(a)*R-3,i*3.2,math.sin(a)*R-3,7+(i%3),4,7+(i%2),S((222,230,246) if i%2 else (150,164,196)))
for i in range(14):
    x=-3+math.sin(i*1.5)*(8-i*.3);Kx(x,2+i*3,-1+math.cos(i*1.1)*4,1.6,4,1.6,S(EL if i%2 else BL))
# 허리 갑옷 스커트
for i,x in enumerate(range(-14,14,4)):Kx(x,28+abs(i-3),-8,4,14-abs(i-3),16,arm(i))
Kx(-13,41,-9,26,4,18,S(GO));Kx(-2,40,9,4,6,1,S(BL))
# 흉갑: 빈 몸 안의 폭풍 핵
Kx(-16,45,-10,32,32,20,arm(1));Kx(-10,50,10,20,22,.6,S(NV));
for i in range(4):
    Kx(-9,52+i*5,10.6,18,2,1.4,S(SV))
Kx(-5,55,11.2,10,10,1,S(BL));Kx(-3,57,12.2,6,6,1,S(EL));Kx(-1,59,13.2,2,2,.8,S((255,255,255)))
Kx(-16,74,-11,32,5,22,S(GO));
# 빈 목: 번개, 떠 있는 투구
for i in range(4):Kx(-1+(i%2)*1.5,79+i*3,-1,2,3.5,2,S(EL if i%2 else BL))
Kx(-10,92,-10,20,18,20,arm(4));Kx(-11,102,-11,22,3,22,S(GO));Kx(-9.5,96,10,19,2.2,.6,S(NV));Kx(-8,96.6,10.4,6,1,.5,S(EL));Kx(2,96.6,10.4,6,1,.5,S(EL));Kx(-1,92,10.2,2,5,.5,S(GO))
for i in range(-8,9,4):Kx(i,92,10.2,1,3,.5,S(SV2))
# 왕관 날개 · 깃
for s in(-1,1):
    for i,(dx,dy,w) in enumerate([(11,104,4),(15,110,4),(20,115,4),(26,118,3),(32,119,3)]):Kx(s*dx-w/2,dy,-1,w,6,3,S(GO if i%2 else SV))
Kx(-2,105,-2,4,16,4,S(GO));Kx(-1,121,-1,2,5,2,S(EL))
# 어깨 구름 견갑
for s in(-1,1):
    x=16 if s>0 else -36
    Kx(x,70,-13,20,14,26,arm(5));Kx(x-1,82,-14,22,3,28,S(GO))
    for j,(a,b,c) in enumerate([(0,84,8),(9,86,10),(4,90,9)]):Kx(x+a,b,-9+j*5,c,6,8,S((236,240,250)))
    for i in range(5):Kx(x+6+i*s*1.2,58-i*8,-3,8,9,7,arm(i+6) if i%2==0 else S(NV))
    for k in(-3,0,3):Kx(x+6+5*s*1.2+k+2,18,-1,2,7,2,S(SV))
# 찢어진 폭풍 망토
for x in range(-26,26,4):
    H=50+((x*7)%17);Kx(x,74-H,-16,4,H,1.5,lambda f,u,v,w,h,x=x:None if v>h-5 and u%2 else (BL if (v+x)%9==0 else NV))
# 벼락 창
Kx(36,0,8,3,120,3,lambda f,u,v,w,h:(GO if v%12<2 else BL));Kx(30,60,8,15,3,3,S(GO))
for i,(dy,w) in enumerate([(120,9),(126,7),(132,5),(138,3),(144,1.5)]):Kx(37.5-w/2,dy,8.5,w,6,2,S(EL))
for s,yy in((-1,128),(1,134)):Kx(37.5+s*7-1,yy,8.5,2,8,2,S(BL))
# 떠 있는 검 넷
for x,z,yy in((-46,0,100),(-54,10,70),(-48,-14,42),(-40,14,24)):
    Kx(x,yy,z,1.6,28,3,S(SV));Kx(x-.4,yy+28,z-2,2.4,2,7,S(GO));Kx(x,yy+30,z+.5,1.6,6,2,S(NV));Kx(x-.2,yy+36,z+.5,2,2,2,S(GO))
# 잡몹 1 구름 파수병
P=[];
def Px(x,y,z,w,h,d,p):P.append(Box(x,y,z,w,h,d,p if callable(p) else S(p)))
for i,(x,z,w,y) in enumerate([(-8,-6,8,2),(-1,-7,9,1),(-7,1,8,0),(1,1,7,3),(-3,5,7,0),(-3,-9,6,3)]):Px(x,y,z,w,9-(i%2),6,S((238,242,250) if i%2 else (204,214,234)))
for i in range(3):Px(-1.5+i,0-i*2,-1,3-i,3,3-i,S((170,182,210)))
Px(-5,12,-3,10,4,6,S(GO));Px(-6,16,-4,12,12,8,arm(2));Px(-3,18,4,6,6,.5,S(NV));Px(-1.5,19.5,4.5,3,3,.5,S(EL))
Px(-3,28,-2,6,3,4,S(SV2));Px(-5,31,-5,10,10,10,arm(3));Px(-5.2,35,5,10.4,1.6,.4,S(NV));Px(-4,35.3,5.3,2.6,.9,.4,S(EL));Px(1.4,35.3,5.3,2.6,.9,.4,S(EL));Px(-5.5,40.5,-5.5,11,1.4,11,S(GO));Px(-1,42,-4,2,6,6,S(BL));Px(-1,47,-6,2,3,8,S(EL))
for s in(-1,1):
    x=6 if s>0 else -10
    Px(x,24,-4,6,6,8,S(GO));Px(x+.5,12,-2,5,12,4,arm(5));
Px(10,0,-1,2,52,2,lambda f,u,v,w,h:GO if v%8<1 else (BL if v%8==4 else SV2));Px(8,52,-1.5,6,2,3,S(GO));Px(9.5,54,-.5,3,9,1,S(EL));Px(10.3,63,-.2,1.5,4,.8,S(EL))
Px(-15,10,-7,1.6,22,16,lambda f,u,v,w,h:GO if u<1 or u>w-2 or v<1 or v>h-2 else (BL if (abs(u-w/2)+abs(v-h/2))<4 else SV))
# 잡몹 2 천둥매
T=[];
def Tx(x,y,z,w,h,d,p):T.append(Box(x,y,z,w,h,d,p if callable(p) else S(p)))
Tx(-5,18,-10,10,9,22,lambda f,u,v,w,h:(BL if (u+v*2)%5==0 else NV if v%3 else (44,52,100)));Tx(-4,23,12,8,8,8,S(NV));Tx(-2.5,25,20,5,2.4,6,S(GO));Tx(-1.5,24,25,3,1.6,3,S(GO2));Tx(-3.6,28,19.4,2,1.4,.6,S(EL));Tx(1.6,28,19.4,2,1.4,.6,S(EL));
for i,(z,h) in enumerate([(14,8),(10,10),(6,8)]):Tx(-1,31,z-1,2,h,2,S(EL if i==0 else BL))
for s in(-1,1):
    for k in range(5):
        x=5+k*7;L=18-k*1.6
        X=x if s>0 else -x-7
        Tx(X,24+k*2.2,-L/2,7,1.6,L,lambda f,u,v,w,h,k=k:None if v>h-3 and u%2 else (NV if (u+v)%6 else BL))
        Tx(X+1,24.5+k*2.2+1.3,-L/2+2,5,.8,L-5,S((60,70,130)))
        Tx(X+2,24.2+k*2.2-1,L/2-.5,1.5,1,2,S(EL))
    Tx(s*3-1.5,12,3,3,7,3,S(GO2));Tx(s*3-3,10,3,6,2,5,S(GO));
for i in range(5):Tx(-5+i*2.4,18,-22,2,1.6,12,S(NV if i%2 else BL));Tx(-5+i*2.4,17,-27,2,1.2,5,S(EL))
for i in range(7):Tx(math.sin(i*1.7)*16-1,6+i*5,math.cos(i)*14-1,1.6,4,1.6,S(EL))
# 잡몹 3 구름 사제
A=[];
def Ax(x,y,z,w,h,d,p):A.append(Box(x,y,z,w,h,d,p if callable(p) else S(p)))
RB=(46,58,120);RB2=(30,40,88)
for i,x in enumerate(range(-9,9,3)):Ax(x,4+(i%2)*2,-7,3,28-(i%2)*2,14,lambda f,u,v,w,h,i=i:None if v<2 and u%2 else (GO if v==h-2 else (RB if (u+i)%3 else RB2)))
for i,(x,z) in enumerate([(-7,-6),(1,-5),(-3,2),(-9,1),(3,2)]):Ax(x,-1+i%2,z,8,6,6,S((236,240,250) if i%2 else (200,212,232)))
Ax(-7,32,-5,14,14,10,lambda f,u,v,w,h:GO if u in(w//2-1,w//2) else RB);Ax(-8,44,-6,16,3,12,S(GO))
Ax(-6,47,-5,12,13,11,S(RB2));Ax(-7,51,-6,14,12,13,lambda f,u,v,w,h:RB if f!='front' else (RB if u<2 or u>w-3 or v>h-3 else VOID));
Ax(-3,55,7.2,2,1.2,.4,S(EL));Ax(1,55,7.2,2,1.2,.4,S(EL));Ax(-1.6,52,7.2,3.2,.8,.4,S((70,80,120)))
Ax(-2,63,-2,4,5,8,S(RB2));Ax(-1,68,-1,2,4,6,S(BL))
for i in range(12):
    a=i*math.pi/6;Ax(15*math.cos(a)-.7,60+15*math.sin(a),-9,1.4,1.4,1.4,S(EL if i%3==0 else BL))
for s in(-1,1):
    x=7 if s>0 else -13
    Ax(x,42,-4,6,7,8,S(GO));Ax(x+.5,30,-3,5,12,6,S(RB));Ax(x+1,25,-2,4,5,4,S(RB2))
    Ax(x+.5,19,-2,5,6,4,lambda f,u,v,w,h:None if u%2==0 and v<3 else SV)
Ax(14,0,-1,2,64,2,lambda f,u,v,w,h:GO2 if v%5 else GO)
for i in range(4):
    a=i*math.pi/2;Ax(15+4*math.cos(a)-.5,72+4*math.sin(a),-.5,2,2,1,S(GO))
Ax(13,69,-2,6,6,4,S(BL));Ax(14.5,70.5,-2.3,3,3,4.6,S(EL));Ax(15.3,71.3,-2.5,1.4,1.4,5,S((255,255,255)))
Ax(-22,50,0,4,4,4,S(BL));Ax(-21,51,-.3,2,2,4.6,S(EL));Ax(-26,34,6,3,3,3,S(BL));
