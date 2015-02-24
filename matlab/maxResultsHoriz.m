function [max_frame, max_index] = maxResultsHoriz( filtered_frame, threshold )
%MAXRESULTS Determine the max response for a given pixel

[h, w, f] = size(filtered_frame);

max_frame = zeros(h,w);
max_index = zeros(h,w);

for i=1:h
    for j=1:w
        max_index(i,j) = 0;
        maxres = threshold;
        for k=1:f
            s = filtered_frame(i,j,k);
            if s > maxres && (k == 1 || k == 3)
                max_index(i,j) = k;
                maxres = s;
                max_frame(i,j) = maxres;
            end
        end
    end
end

end

