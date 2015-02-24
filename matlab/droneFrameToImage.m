function droneimg = droneFrameToImage( frame, height, width )
%DRONEFRAMETOIMAGE converts row-major drone frame to column-major 2D image

len = size(frame,1);
cdim = len / (height * width);

if cdim == 1
    % grayscale or color channel
    droneimg = reshape(frame,width,height);
    droneimg = droneimg';
else
    droneimg = zeros(height, width, cdim);
    idx = 1;
    for i=1:height
        for j=1:width
            droneimg(i,j,1:cdim) = frame(idx:idx+cdim-1);
            idx = idx + cdim;
        end
    end
end

end

