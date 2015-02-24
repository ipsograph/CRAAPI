function droneimg = droneFrameToRgbImage( frame, height, width )
%DRONEFRAMETORGBIMAGE converts frame directly to RGB image (saves memory)

droneimg = zeros(height, width, 3);
idx = 1;
for i=1:height
    for j=1:width
        droneimg(i,j,1:3) = typecast(frame(idx:idx+2),'uint8');
        %droneimg(i,j,2) = typecast(frame(idx+1),'uint8');
        %droneimg(i,j,1) = typecast(frame(idx+2),'uint8');
        idx = idx + 3;
    end
end

droneimg = droneimg ./ 255.0;

end

