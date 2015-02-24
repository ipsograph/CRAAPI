function video = createJavaVideo( )
%CREATEJAVAVIDEO Creates and returns a Java VideoCapture object
%   See Java class 'org.craapi.drone.matlab.VideoCapture'
%
%   Must be added to classpath: (via javaddpath)
%    craapi_ardrone2_jre6.jar
%    slf4j-api-1.7.2.jar
%    xuggle-xuggler-5.4.jar
%

video = javaObject('org.craapi.drone.matlab.VideoCapture');

end

