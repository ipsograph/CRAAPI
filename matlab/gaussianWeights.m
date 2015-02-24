function weighted_result = gaussianWeights( result, sigma )
%OPPONENTNORM Summary of this function goes here
%   Detailed explanation goes here

[h, w, c] = size(result);

weighted_result = zeros(h, w, c);
k = fspecial('Gaussian',[h w],sigma);
for i=1:c
    temp = result(:,:,i);
    temp = temp .* k;
    temp = temp ./ norm(temp(:));
    weighted_result(:,:,i) = temp;
end

end

