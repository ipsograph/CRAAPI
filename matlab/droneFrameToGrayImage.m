function grayimg = droneFrameToGrayImage( frame, height, width )
%DRONEFRAMETOGRAYIMAGE converts frame directly to grayscale image

% color channel weights
rw = 0.3;
gw = 0.59;
bw = 0.11;

len = size(frame,1);

grayimg = (double(typecast(frame(1:3:len-2),'uint8')) * rw) + (double(typecast(frame(2:3:len-1),'uint8')) * gw) + (double(typecast(frame(3:3:len),'uint8')) * bw);


% idx = 1;
% for i=1:height
%     for j=1:width
%         wsum = typecast(frame(idx),'uint8') * rw;
%         wsum = wsum + typecast(frame(idx+1),'uint8') * gw;
%         wsum = wsum + typecast(frame(idx+2),'uint8') * bw;
%         grayimg(i,j) = wsum;
%         idx = idx + 3;
%     end
% end

grayimg = grayimg ./ 255.0;

end

