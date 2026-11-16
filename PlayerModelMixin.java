package dev.mcdance.mixin;

import dev.mcdance.PoseApplier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after vanilla has posed the player model for this frame and, while a dance is active,
 * overwrites the part transforms. Vanilla resets every part at the start of the next frame,
 * so nothing is ever permanently changed.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void mcdance$applyDance(CallbackInfo ci) {
        PoseApplier.apply((HumanoidModel<?>) (Object) this);
    }
}
