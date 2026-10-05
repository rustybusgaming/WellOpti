package dev.wellopti.mixin;

import dev.wellopti.TickThrottle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
	private void wellopti$throttleHiddenEntities(Entity entity, CallbackInfo ci) {
		if (TickThrottle.shouldSkipTick(entity, (ClientLevel) (Object) this)) {
			ci.cancel();
		}
	}
}
