function filtered_videos = apply_filters(videos, filters)
% Compute Normalized correlation between any image and any filter

n_videos = length(videos);
n_filters = length(filters);
filtered_videos = cell(1,n_videos);

% for each video...
for v=1:n_videos
    
    video = videos{v};
    [vh, vw, n_frames] = size(video);
    
    % assume all filters same
    filter = filters{1};
    [fh, fw, time_dim] = size(filter);
    n_filtered_frames = n_frames-time_dim+1;
    filtered_frames = zeros(vh-fh+1, vw-fw+1, n_filtered_frames, n_filters);
    
    % for each filter...
    for ff=1:n_filters
        filter = filters{ff};
        
        % apply the filter to each frame
        for f=1:n_filtered_frames
            frame = video(:,:,f:f+time_dim-1);
            res = convn(frame, filter, 'valid');
            res = res .^ 2;
            res = res ./ norm(res);
            filtered_frames(:,:,f,ff) = res;
        end
    end
    
    filtered_videos{v} = filtered_frames;
end

end

