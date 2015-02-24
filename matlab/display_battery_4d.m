function display_battery_4d( width, fig, battery, dense_flag )

w = width;
nrows = size(battery,1);

figure(fig);

if dense_flag == 1
    
    sample = battery{1};
    dims = size(sample);
    dense = zeros(dims(2)*h,dims(1)*w);
    for p=1:nplots
        
        r = floor((p-1)/w);
        c = mod(p-1,w);
        
        rpx = r*dims(1)+1;
        cpx = c*dims(2)+1;
        
        dense(rpx:rpx+dims(1)-1,cpx:cpx+dims(2)-1) = battery{p};
    end
    
    dense = dense - min(dense(:));
    dense = dense ./ max(dense(:));
    imshow(dense);
    
else
    idx = 1;
    for p=1:nrows
        
        b = battery{p};
        b = b - min(b(:));
        b = b ./ max(b(:));
        ncols = size(b,3);
        
        for sp=1:ncols
    
            subplot(nrows,ncols,idx);
            imshow(b(:,:,sp));
            idx = idx + 1;
        
        end

    end
end


end