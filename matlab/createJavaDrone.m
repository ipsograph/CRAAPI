function drone = createJavaDrone( simulation )
%INITJAVADRONE Creates and returns a Java MatlabDrone object
%   See Java class 'org.craapi.drone.matlab.MatlabDrone'
%   in craapi_ardrone2_jre6.jar file (must be added to javaclasspath 
%   via javaaddpath)
%
%   simulation=1 : if 1, then the drone will be in sim mode only
%   simulation=0 : if 0, then a WIFI connection is used (real drone)

drone = javaObject('org.craapi.drone.matlab.MatlabDrone');
drone.setSimulation(simulation);

end

