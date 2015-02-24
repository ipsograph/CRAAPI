function filtered_frames = filterFrameSequence( video, filters )
%FILTERFRAMESEQUENCE Apply a battery of spatio-temporal filters to a
%sequence of images.

n_filters = length(filters);
[vh, vw, n_frames] = size(video);

% assume all filters same
filter = filters{1};
[fh, fw, time_dim] = size(filter);

% our filters must match our sample...
assert(time_dim == n_frames,sprintf('The number of frames (%i) and filter time dimension (%i) must match',n_frames,n_filters));

% result
filtered_frames = zeros(vh-fh+1, vw-fw+1, n_filters);

% for each filter...
for ff=1:n_filters
    filter = filters{ff};

    % apply the filter to frame sequence
    %frame = video(:,:,f:f+time_dim-1);
    res = convn(video, filter, 'valid');
    res = res .^ 2;
    res = res ./ norm(res);
    filtered_frames(:,:,ff) = res;
end

end

