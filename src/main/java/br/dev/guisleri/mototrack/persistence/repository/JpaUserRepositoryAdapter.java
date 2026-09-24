package br.dev.guisleri.mototrack.persistence.repository;

import br.dev.guisleri.mototrack.model.User;
import br.dev.guisleri.mototrack.persistence.entity.UserEntity;
import br.dev.guisleri.mototrack.persistence.mapper.UserMapper;
import br.dev.guisleri.mototrack.repository.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    public JpaUserRepositoryAdapter(
            SpringDataUserRepository springDataUserRepository,
            UserMapper userMapper
    ) {
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = false)
    public User save(User user) {
        UserEntity userEntity =
                userMapper.toEntity(user);

        UserEntity savedUserEntity = springDataUserRepository.save(userEntity);

        return userMapper.toDomain(savedUserEntity);
    }

    @Override
    public Optional<User> findByEmail(String userEmail) {
        return springDataUserRepository.findByEmail(userEmail)
                .map(userMapper::toDomain);
    }

}
