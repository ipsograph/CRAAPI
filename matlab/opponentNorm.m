function norm_result = opponentNorm( result, op_chan_idx, d )
%OPPONENTNORM Summary of this function goes here
%   Detailed explanation goes here

[h, w, c] = size(result);

norm_result = zeros(h, w, c);
for i=1:c
    temp = result(:,:,i) ./ (result(:,:,op_chan_idx(i)) + d);
    temp = temp ./ norm(temp(:));
    norm_result(:,:,i) = temp;
end

end

