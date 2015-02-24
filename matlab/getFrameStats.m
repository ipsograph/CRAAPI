function [ height, width, colorspace ] = getFrameStats( framebytes )
%GETFRAMESTATS Return the properties of the frame
%   height     : height of the frame in pixels
%   width      : width of the frame in pixels
%   colorspace : the colorpsace (e.g. 'bgr24')
%
% This function is not very robust...makes many assumptions

dims = size(framebytes);
if dims(1) == 691200
    height = 360;
    width = 640;
    colorspace = 'bgr24';
elseif dims(1) == 1555200
    height = 540;
    width = 960;
    colorspace = 'bgr24';
elseif dims(1) == 2764800
    height = 720;
    width = 1280;
    colorspace = 'bgr24';
elseif dims(1) == 311040
    height = 240;
    width = 432;
    colorspace = 'bgr24';
else
    height = -1;
    width = -1;
    colorspace = 'n/a';
end

