function rgbframe = droneFrameToRgb( frame )
%DRONEFRAMETORGB Converts an ARDrone frame to an RGB double array

len = size(frame,1);
rgbframe = zeros(len);
rgbframe(1:len) = typecast(frame(1:len), 'uint8');

% below is for BGR
%rgbframe(3:3:len) = typecast(frame(1:3:len-2),'uint8');
%rgbframe(2:3:len-1) = typecast(frame(2:3:len-1), 'uint8');
%rgbframe(1:3:len-2) = typecast(frame(3:3:len), 'uint8');

rgbframe = rgbframe ./ 255.0;

end

