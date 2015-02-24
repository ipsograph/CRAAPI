function [grid_response, winner_idx] = gridResults( original, response, response_idx, index_count, grid_size, threshold, mode )

[h, w] = size(response_idx);

% index breakdown
% 0=null
% 1=index 1 dominates
% 2=index 2 dominates
% ...
% n=index n dominates

% todo/maybe
% n+1=index 1 and 2 combined
% n+2=index 2 and 3 combined
% ...
% 2n-1=index n-1 and index n combined
% 

gh = h/grid_size;
gw = w/grid_size;
grid_response = zeros(h,w);
gridthresh = grid_size * grid_size * threshold;
winidx = zeros(index_count);
for i=1:gh
    for j=1:gw
        hidx = (grid_size*(i-1))+1;
        widx = (grid_size*(j-1))+1;
        if strcmp(mode,'hist') == 1
            bincounts = hist(response_idx(hidx:hidx+grid_size-1,widx:widx+grid_size-1),index_count);
            maxres = gridthresh;
            maxidx = 0;
            for b=2:index_count
                bsamp = bincounts(b);
                if bsamp > maxres
                    maxres = bsamp;
                    maxidx = b-1;
                end
            end
            grid_response(hidx:hidx+grid_size-1,widx:widx+grid_size-1)=maxidx;
            winidx(maxidx+1) = winidx(maxidx+1) + 1;
        elseif strcmp(mode,'max') == 1
            maxres = threshold;
            maxidx = 0;
            for ii=hidx:hidx+grid_size-1
                for jj=widx:widx+grid_size-1
                    if response(ii,jj) > maxres
                        maxres = response(ii,jj);
                        maxidx = response_idx(ii,jj);
                    end
                end
            end
            grid_response(hidx:hidx+grid_size-1,widx:widx+grid_size-1)=maxidx;
            winidx(maxidx+1) = winidx(maxidx+1) + 1;
        elseif strcmp(mode,'sum') == 1
            maxres = 0;
            maxidx = 0;
            tot = 0;
            for b=2:index_count
                sumt = sum(original(hidx:hidx+grid_size-1,widx:widx+grid_size-1,b-1));
                if sumt > maxres
                    maxres = sumt;
                    maxidx = b-1;
                end
                tot = tot + sumt;
            end
            if (maxres / tot) < threshold
                maxidx = 0;
            end
            grid_response(hidx:hidx+grid_size-1,widx:widx+grid_size-1)=maxidx;
            winidx(maxidx+1) = winidx(maxidx+1) + 1;
        elseif strcmp(mode,'sumHoriz') == 1 && i/gh > 0.2 && i/gh < 0.8 && j/gw > 0.2 && j/gw < 0.8
            maxres = 0;
            maxidx = 0;
            tot = 0.00000000001;
            for b=2:index_count
                sumt = sum(original(hidx:hidx+grid_size-1,widx:widx+grid_size-1,b-1));
                if sumt > maxres
                    maxres = sumt;
                    maxidx = b-1;
                end
                tot = tot + sumt;
            end
            
            if maxidx == 2 || maxidx == 4
                maxidx = 0;
            elseif (maxres / tot) < threshold
                maxidx = 0;
            end
            grid_response(hidx:hidx+grid_size-1,widx:widx+grid_size-1)=maxidx;
            winidx(maxidx+1) = winidx(maxidx+1) + 1;
        end
    end
end

maxres = 0.1 * length(winidx(:));
winner_idx = 1;
for idx=2:index_count
    if winidx(idx) > maxres
        maxres = winidx(idx);
        winner_idx = idx;
    end
end

end

