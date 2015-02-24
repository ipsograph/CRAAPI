function grayframe = droneFrameToGray( frame )
%BGR24FRAMETOGRAY Converts an ARDrone frame to a grayscale double array

% color channel weights
rw = 0.3;
gw = 0.59;
bw = 0.11;

% convert to grayscale (I am sure there is a function for this, but w/e)
len = size(frame,1);
grayframe = (double(typecast(frame(1:3:len-2),'uint8')) * rw) + (double(typecast(frame(2:3:len-1),'uint8')) * gw) + (double(typecast(frame(3:3:len),'uint8')) * bw);

%grayframe = zeros(len/3);
%grayframe(1:len) = typecast(frame(1:len), 'uint8');

% below is for BGR
%bc = temp(1:3:len-2) * bw;
%gc = temp(2:3:len-1) * gw;
%rc = temp(3:3:len) * rw;

% RGB
%grayframe(:) = (grayframe(1:3:len-2) * rw) + (grayframe(2:3:len-1) * gw) + (grayframe(3:3:len) * bw);
%grayframe(2:3:len-1) = grayframe(2:3:len-1) * gw;
%grayframe(3:3:len) = grayframe(3:3:len) * bw;
%gc = temp(2:3:len-1) * gw;
%bc = temp(3:3:len) * bw;

%grayframe = bc + gc + rc;
grayframe = grayframe ./ 255.0;

end

