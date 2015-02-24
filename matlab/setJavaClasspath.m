function setJavaClasspath( projectdir, videoFlag, addNativeJarsFlag )
%SETJAVACLASSPATH Sets up classpath 
%   Adds jars to your classpath (needed for drone/video)
%
%   Presumes all jars are in the same 'java' directory within the given
%   projectdir (string path)
%
%   projectdir : the directory containing the 'java' directory in which the
%     jars are located
%   videoFlag  : if '1', video libraries are added (required for video)
%   addNativeJarsFlag : if '1', native jars are added (may not be
%     compatible with all operating systems (incomplete)
%
% TODO - need to add support for different OS
javaaddpath(sprintf('%s/java/craapi_common.jar', projectdir));
javaaddpath(sprintf('%s/java/craapi_video.jar', projectdir));
javaaddpath(sprintf('%s/java/craapi_ardrone2_jre6.jar', projectdir));

if videoFlag == 1
    javaaddpath(sprintf('%s/java/slf4j-api-1.7.2.jar', projectdir));
    javaaddpath(sprintf('%s/java/xuggle-xuggler-5.4.jar', projectdir));
%     if addNativeJarsFlag == 1
%         javaaddpath(sprintf('%s/java/xuggle-xuggler-arch-x86_64-w64-mingw32.jar', projectdir));
%     end
%     javaaddpath(sprintf('%s/java/xuggle-xuggler-noarch-5.4.jar', projectdir));
end

end

